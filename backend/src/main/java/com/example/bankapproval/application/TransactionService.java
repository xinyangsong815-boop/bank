package com.example.bankapproval.application;

import com.example.bankapproval.api.dto.*;
import com.example.bankapproval.common.BizException;
import com.example.bankapproval.common.OptimisticLockException;
import com.example.bankapproval.common.PageResult;
import com.example.bankapproval.domain.approval.ApprovalContext;
import com.example.bankapproval.domain.approval.ApprovalRuleChain;
import com.example.bankapproval.domain.model.*;
import com.example.bankapproval.domain.repository.*;
import com.example.bankapproval.domain.risk.RiskContext;
import com.example.bankapproval.domain.risk.RiskEngine;
import com.example.bankapproval.domain.risk.RiskResult;
import com.example.bankapproval.domain.state.StateTransition;
import com.example.bankapproval.domain.state.TransactionEvent;
import com.example.bankapproval.domain.state.TransactionStateMachine;
import com.example.bankapproval.infrastructure.crypto.CryptoService;
import com.example.bankapproval.infrastructure.crypto.SensitiveMasker;
import com.example.bankapproval.security.DemoPrincipal;
import com.example.bankapproval.security.SecurityUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final ApprovalRecordRepository approvals;
    private final AuditLogRepository audits;
    private final CryptoService crypto;
    private final RiskEngine riskEngine;
    private final TransactionStateMachine stateMachine;
    private final ApprovalRuleChain approvalRules;
    private final TransactionDtoMapper dtoMapper;

    public TransactionService(TransactionRepository transactions, ApprovalRecordRepository approvals,
                              AuditLogRepository audits, CryptoService crypto, RiskEngine riskEngine,
                              TransactionStateMachine stateMachine, ApprovalRuleChain approvalRules,
                              TransactionDtoMapper dtoMapper) {
        this.transactions = transactions;
        this.approvals = approvals;
        this.audits = audits;
        this.crypto = crypto;
        this.riskEngine = riskEngine;
        this.stateMachine = stateMachine;
        this.approvalRules = approvalRules;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('APPLICANT','ADMIN')")
    public TransactionResponse create(CreateTransactionRequest request, String ip) {
        DemoPrincipal user = SecurityUtils.currentUser();
        TransactionApplication tx = new TransactionApplication();
        tx.setApplicationNo(newApplicationNo());
        tx.setApplicantId(user.getUserId());
        tx.setApplicantName(user.getUserName());
        tx.setDepartmentId(user.getDepartmentId());
        applySensitiveAndBusinessFields(tx, request.getMobile(), request.getIdCard(), request.getAccount(),
                request.getCounterpartyAccount(), request.getAmount(), request.getCurrency(), request.getPurpose());
        RiskResult risk = riskEngine.evaluate(new RiskContext(request.getAmount(), request.getPurpose(), request.getCounterpartyAccount()));
        tx.setRiskScore(risk.getScore());
        tx.setRiskLevel(risk.getLevel());
        tx.setStatus(TransactionStatus.DRAFT);
        tx.setCurrentLevel(0);
        tx.setVersion(0L);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(tx.getCreatedAt());
        transactions.insert(tx);
        audit(tx.getId(), user, "CREATE", "创建交易草稿 " + tx.getApplicationNo(), ip);
        return toResponse(tx);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public PageResult<TransactionResponse> list(TransactionQueryRequest request) {
        DemoPrincipal user = SecurityUtils.currentUser();
        if (request.getStartDate() != null && request.getEndDate() != null
                && request.getStartDate().isAfter(request.getEndDate())) {
            throw new BizException("INVALID_DATE_RANGE", "开始日期不能晚于结束日期");
        }
        TransactionQuery query = new TransactionQuery();
        query.setKeyword(trimToNull(request.getKeyword()));
        query.setStatus(request.getStatus());
        query.setStartAt(request.getStartDate() == null ? null : request.getStartDate().atStartOfDay());
        query.setEndExclusive(request.getEndDate() == null ? null : request.getEndDate().plusDays(1).atStartOfDay());
        query.setOffset((request.getPage() - 1) * request.getSize());
        query.setLimit(request.getSize());
        applyDataScope(query, user);
        List<TransactionResponse> records = transactions.findPage(query).stream().map(this::toResponse).collect(Collectors.toList());
        return new PageResult<TransactionResponse>(records, transactions.count(query), request.getPage(), request.getSize());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public TransactionResponse get(Long id) {
        TransactionApplication tx = required(id);
        assertCanView(tx, SecurityUtils.currentUser());
        return toResponse(tx);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('APPLICANT','ADMIN')")
    public TransactionResponse submit(Long id, SubmitRequest request, String ip) {
        DemoPrincipal user = SecurityUtils.currentUser();
        TransactionApplication tx = required(id);
        assertOwner(tx, user);
        assertVersion(tx, request.getVersion());
        StateTransition transition = stateMachine.transit(tx.getStatus(), TransactionEvent.SUBMIT);
        RiskResult risk = evaluate(tx);
        conditionalStatusUpdate(tx, request.getVersion(), transition, null, risk);
        audit(id, user, "SUBMIT", "提交至一级审批", ip);
        return toResponse(required(id));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('APPLICANT','ADMIN')")
    public TransactionResponse resubmit(Long id, ResubmitTransactionRequest request, String ip) {
        DemoPrincipal user = SecurityUtils.currentUser();
        TransactionApplication existing = required(id);
        assertOwner(existing, user);
        assertVersion(existing, request.getVersion());
        StateTransition transition = stateMachine.transit(existing.getStatus(), TransactionEvent.RESUBMIT);
        applySensitiveAndBusinessFields(existing, request.getMobile(), request.getIdCard(), request.getAccount(),
                request.getCounterpartyAccount(), request.getAmount(), request.getCurrency(), request.getPurpose());
        RiskResult risk = riskEngine.evaluate(new RiskContext(request.getAmount(), request.getPurpose(), request.getCounterpartyAccount()));
        existing.setRiskScore(risk.getScore());
        existing.setRiskLevel(risk.getLevel());
        existing.setStatus(transition.getNextStatus());
        existing.setCurrentLevel(transition.getNextLevel());
        if (transactions.updateForResubmit(existing, request.getVersion(), TransactionStatus.REJECTED.name()) != 1) {
            throw new OptimisticLockException();
        }
        audit(id, user, "RESUBMIT", "修改后重新提交至一级审批", ip);
        return toResponse(required(id));
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('APPROVER_L1','APPROVER_L2','APPROVER_L3','APPROVER_L4')")
    public TransactionResponse approve(Long id, ApprovalRequest request, String ip) {
        return decide(id, request.getVersion(), request.getComment(), false, ip);
    }

    @Transactional(rollbackFor = Exception.class)
    @PreAuthorize("hasAnyRole('APPROVER_L1','APPROVER_L2','APPROVER_L3','APPROVER_L4')")
    public TransactionResponse reject(Long id, RejectRequest request, String ip) {
        return decide(id, request.getVersion(), request.getReason(), true, ip);
    }

    private TransactionResponse decide(Long id, long version, String comment, boolean reject, String ip) {
        DemoPrincipal user = SecurityUtils.currentUser();
        TransactionApplication tx = required(id);
        assertCanView(tx, user);
        // 先检查旧页面的版本，再判断审批级别；重复点击也稳定得到 409。
        // 此检查只改善错误语义，真正的并发保护仍由 SQL 的 version+status 条件完成。
        assertVersion(tx, version);
        approvalRules.check(new ApprovalContext(tx, user));
        TransactionEvent event = reject ? TransactionEvent.REJECT : TransactionEvent.APPROVE;
        StateTransition transition = stateMachine.transit(tx.getStatus(), event);
        RiskResult risk = evaluate(tx);
        conditionalStatusUpdate(tx, version, transition, reject ? comment : null, risk);

        ApprovalRecord record = new ApprovalRecord();
        record.setTransactionId(id);
        record.setLevel(tx.getCurrentLevel());
        record.setApproverId(user.getUserId());
        record.setApproverName(user.getUserName());
        record.setAction(reject ? "REJECT" : "APPROVE");
        record.setStatusBefore(tx.getStatus().name());
        record.setStatusAfter(transition.getNextStatus().name());
        record.setComment(comment);
        record.setCreatedAt(LocalDateTime.now());
        approvals.insert(record);
        audit(id, user, reject ? "REJECT" : "APPROVE",
                "第 " + tx.getCurrentLevel() + " 级" + (reject ? "驳回" : "通过"), ip);
        return toResponse(required(id));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<ApprovalRecord> approvalHistory(Long id) {
        TransactionApplication tx = required(id);
        assertCanView(tx, SecurityUtils.currentUser());
        return approvals.findByTransactionId(id);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public List<AuditLog> auditHistory(Long id) {
        TransactionApplication tx = required(id);
        assertCanView(tx, SecurityUtils.currentUser());
        return audits.findByTransactionId(id);
    }

    private void conditionalStatusUpdate(TransactionApplication tx, long expectedVersion, StateTransition transition,
                                         String rejectionReason, RiskResult risk) {
        if (transactions.updateStatus(tx.getId(), expectedVersion, tx.getStatus().name(),
                transition.getNextStatus().name(), transition.getNextLevel(), rejectionReason,
                risk.getScore(), risk.getLevel()) != 1) {
            throw new OptimisticLockException();
        }
    }

    private void assertVersion(TransactionApplication tx, long expectedVersion) {
        if (tx.getVersion() != expectedVersion) throw new OptimisticLockException();
    }

    private RiskResult evaluate(TransactionApplication tx) {
        return riskEngine.evaluate(new RiskContext(tx.getAmount(), tx.getPurpose(),
                crypto.decrypt(tx.getCounterpartyAccountCiphertext())));
    }

    private void applySensitiveAndBusinessFields(TransactionApplication tx, String mobile, String idCard,
                                                  String account, String counterpartyAccount,
                                                  java.math.BigDecimal amount, String currency, String purpose) {
        tx.setMobileCiphertext(crypto.encrypt(mobile));
        tx.setIdCardCiphertext(crypto.encrypt(idCard.toUpperCase(Locale.ROOT)));
        tx.setAccountCiphertext(crypto.encrypt(account));
        tx.setCounterpartyAccountCiphertext(crypto.encrypt(counterpartyAccount));
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setPurpose(purpose.trim());
    }

    private TransactionResponse toResponse(TransactionApplication tx) {
        TransactionResponse response = dtoMapper.toResponse(tx);
        response.setMobileMasked(SensitiveMasker.mobile(crypto.decrypt(tx.getMobileCiphertext())));
        response.setIdCardMasked(SensitiveMasker.idCard(crypto.decrypt(tx.getIdCardCiphertext())));
        response.setAccountMasked(SensitiveMasker.account(crypto.decrypt(tx.getAccountCiphertext())));
        response.setCounterpartyAccountMasked(SensitiveMasker.account(crypto.decrypt(tx.getCounterpartyAccountCiphertext())));
        return response;
    }

    private TransactionApplication required(Long id) {
        TransactionApplication tx = transactions.findById(id);
        if (tx == null) throw new BizException("NOT_FOUND", "交易申请不存在");
        return tx;
    }

    private void assertOwner(TransactionApplication tx, DemoPrincipal user) {
        if (user.getRole() != UserRole.ADMIN && !tx.getApplicantId().equals(user.getUserId())) {
            throw new BizException("DATA_SCOPE_DENIED", "只能操作本人申请");
        }
    }

    private void assertCanView(TransactionApplication tx, DemoPrincipal user) {
        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.AUDITOR) return;
        if (user.getRole() == UserRole.APPLICANT) {
            assertOwner(tx, user);
        } else if (!tx.getDepartmentId().equals(user.getDepartmentId())) {
            throw new BizException("DATA_SCOPE_DENIED", "只能查看本部门申请");
        }
    }

    private void applyDataScope(TransactionQuery query, DemoPrincipal user) {
        if (user.getRole() == UserRole.APPLICANT) query.setApplicantId(user.getUserId());
        else if (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.AUDITOR) query.setDepartmentId(user.getDepartmentId());
    }

    private void audit(Long transactionId, DemoPrincipal user, String operation, String detail, String ip) {
        AuditLog log = new AuditLog();
        log.setTransactionId(transactionId);
        log.setOperatorId(user.getUserId());
        log.setOperatorName(user.getUserName());
        log.setOperation(operation);
        log.setDetail(detail);
        log.setIpAddress(ip);
        log.setCreatedAt(LocalDateTime.now());
        audits.insert(log);
    }

    private String newApplicationNo() {
        return "TX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }
}

package com.example.bankapproval.application;

import com.example.bankapproval.api.dto.*;
import com.example.bankapproval.domain.model.TransactionStatus;
import com.example.bankapproval.domain.model.UserRole;
import com.example.bankapproval.security.DemoPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionServiceFlowIntegrationTest {
    @Autowired private TransactionService service;

    @AfterEach
    void clearContext() { SecurityContextHolder.clearContext(); }

    @Test
    void applicationPassesFourLevelsInOrder() {
        authenticate("U1001", "申请人", UserRole.APPLICANT);
        TransactionResponse tx = service.create(createRequest(), "127.0.0.1");
        tx = service.submit(tx.getId(), submit(tx.getVersion()), "127.0.0.1");
        assertEquals(TransactionStatus.PENDING_L1, tx.getStatus());

        for (int level = 1; level <= 4; level++) {
            authenticate("A200" + level, level + "级审批员", UserRole.valueOf("APPROVER_L" + level));
            ApprovalRequest request = new ApprovalRequest();
            request.setVersion(tx.getVersion());
            request.setComment("同意");
            tx = service.approve(tx.getId(), request, "127.0.0.1");
        }
        assertEquals(TransactionStatus.APPROVED, tx.getStatus());
        assertEquals(4, service.approvalHistory(tx.getId()).size());
        assertEquals(6, service.auditHistory(tx.getId()).size()); // 创建、提交、四次审批
    }

    @Test
    void rejectedApplicationCanBeEditedAndResubmitted() {
        authenticate("U1001", "申请人", UserRole.APPLICANT);
        TransactionResponse tx = service.create(createRequest(), "127.0.0.1");
        tx = service.submit(tx.getId(), submit(tx.getVersion()), "127.0.0.1");

        authenticate("A2001", "一级审批员", UserRole.APPROVER_L1);
        RejectRequest reject = new RejectRequest();
        reject.setVersion(tx.getVersion());
        reject.setReason("请补充用途");
        tx = service.reject(tx.getId(), reject, "127.0.0.1");
        assertEquals(TransactionStatus.REJECTED, tx.getStatus());

        authenticate("U1001", "申请人", UserRole.APPLICANT);
        ResubmitTransactionRequest resubmit = new ResubmitTransactionRequest();
        copy(createRequest(), resubmit);
        resubmit.setPurpose("补充后的设备采购用途");
        resubmit.setVersion(tx.getVersion());
        tx = service.resubmit(tx.getId(), resubmit, "127.0.0.1");
        assertEquals(TransactionStatus.PENDING_L1, tx.getStatus());
        assertNull(tx.getRejectionReason());
    }

    private void authenticate(String id, String name, UserRole role) {
        DemoPrincipal principal = new DemoPrincipal(id, name, "D001", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private SubmitRequest submit(Long version) {
        SubmitRequest request = new SubmitRequest();
        request.setVersion(version);
        return request;
    }

    private CreateTransactionRequest createRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setMobile("13800138000");
        request.setIdCard("110101199001011234");
        request.setAccount("6222021234567890");
        request.setCounterpartyAccount("6217001234567890");
        request.setAmount(new BigDecimal("88000.00"));
        request.setCurrency("CNY");
        request.setPurpose("设备采购");
        return request;
    }

    private void copy(CreateTransactionRequest source, ResubmitTransactionRequest target) {
        target.setMobile(source.getMobile());
        target.setIdCard(source.getIdCard());
        target.setAccount(source.getAccount());
        target.setCounterpartyAccount(source.getCounterpartyAccount());
        target.setAmount(source.getAmount());
        target.setCurrency(source.getCurrency());
        target.setPurpose(source.getPurpose());
    }
}

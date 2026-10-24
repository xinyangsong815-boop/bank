package com.example.bankapproval.application;

import com.example.bankapproval.api.dto.*;
import com.example.bankapproval.common.OptimisticLockException;
import com.example.bankapproval.domain.model.TransactionStatus;
import com.example.bankapproval.domain.model.UserRole;
import com.example.bankapproval.domain.repository.AuditLogRepository;
import com.example.bankapproval.security.DemoPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

/** 不给测试方法套事务：让 Service 的真实提交、回滚以及两个线程竞争能被观察到。 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:atomicity;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
@ActiveProfiles("test")
class TransactionAtomicityIntegrationTest {
    @Autowired private TransactionService service;
    @MockBean private AuditLogRepository auditLogRepository;

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    private void login(String id, UserRole role) {
        DemoPrincipal principal = new DemoPrincipal(id, id, "D001", role);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private TransactionResponse pending() {
        login("atomic-applicant", UserRole.APPLICANT);
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setMobile("13800138000"); request.setIdCard("110101199001011234");
        request.setAccount("6222021234567890"); request.setCounterpartyAccount("6217001234567890");
        request.setAmount(new BigDecimal("10000.00")); request.setCurrency("CNY"); request.setPurpose("采购");
        TransactionResponse tx = service.create(request, "127.0.0.1");
        SubmitRequest submit = new SubmitRequest(); submit.setVersion(tx.getVersion());
        return service.submit(tx.getId(), submit, "127.0.0.1");
    }

    private ApprovalRequest decision(long version) {
        ApprovalRequest request = new ApprovalRequest(); request.setVersion(version); request.setComment("同意");
        return request;
    }

    @Test void auditFailureRollsBackStatusAndApprovalTogether() {
        TransactionResponse tx = pending();
        login("atomic-approver", UserRole.APPROVER_L1);
        doThrow(new IllegalStateException("模拟审计存储故障")).when(auditLogRepository).insert(any());
        assertThrows(IllegalStateException.class, () -> service.approve(tx.getId(), decision(tx.getVersion()), "127.0.0.1"));
        TransactionResponse unchanged = service.get(tx.getId());
        assertEquals(TransactionStatus.PENDING_L1, unchanged.getStatus());
        assertEquals(tx.getVersion(), unchanged.getVersion());
        assertTrue(service.approvalHistory(tx.getId()).isEmpty());
    }

    @Test void twoConcurrentApprovalsProduceExactlyOneApproval() throws Exception {
        TransactionResponse tx = pending();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> worker = () -> {
            login("concurrent-approver", UserRole.APPROVER_L1);
            ready.countDown();
            try {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                service.approve(tx.getId(), decision(tx.getVersion()), "127.0.0.1");
                return true;
            } catch (OptimisticLockException expected) { return false; }
            finally { SecurityContextHolder.clearContext(); }
        };
        try {
            Future<Boolean> first = executor.submit(worker), second = executor.submit(worker);
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            int successes = (first.get(10, TimeUnit.SECONDS) ? 1 : 0) + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, successes);
            login("audit", UserRole.AUDITOR);
            assertEquals(TransactionStatus.PENDING_L2, service.get(tx.getId()).getStatus());
            assertEquals(1, service.approvalHistory(tx.getId()).size());
        } finally { start.countDown(); executor.shutdownNow(); }
    }
}

package com.example.bankapproval.infrastructure.persistence;

import com.example.bankapproval.domain.model.TransactionApplication;
import com.example.bankapproval.domain.model.TransactionStatus;
import com.example.bankapproval.domain.repository.TransactionQuery;
import com.example.bankapproval.domain.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MybatisRepositoryIntegrationTest {
    @Autowired private TransactionRepository repository;

    @Test
    void conditionalUpdatePreventsLostUpdate() {
        TransactionApplication tx = fixture();
        repository.insert(tx);
        assertNotNull(tx.getId());
        assertEquals(1, repository.updateStatus(tx.getId(), 0L, "DRAFT", "PENDING_L1", 1, null, 10, "LOW"));
        assertEquals(0, repository.updateStatus(tx.getId(), 0L, "DRAFT", "PENDING_L1", 1, null, 10, "LOW"));
        assertEquals(1L, repository.findById(tx.getId()).getVersion());
        TransactionQuery query = new TransactionQuery();
        query.setApplicantId("U1001");
        query.setOffset(0);
        query.setLimit(10);
        assertEquals(1, repository.findPage(query).size());
    }

    private TransactionApplication fixture() {
        TransactionApplication tx = new TransactionApplication();
        tx.setApplicationNo("TEST-001");
        tx.setApplicantId("U1001");
        tx.setApplicantName("测试申请人");
        tx.setDepartmentId("D001");
        tx.setMobileCiphertext("cipher-mobile");
        tx.setIdCardCiphertext("cipher-id");
        tx.setAccountCiphertext("cipher-account");
        tx.setCounterpartyAccountCiphertext("cipher-counterparty");
        tx.setAmount(new BigDecimal("1000.00"));
        tx.setCurrency("CNY");
        tx.setPurpose("测试");
        tx.setRiskScore(0);
        tx.setRiskLevel("LOW");
        tx.setStatus(TransactionStatus.DRAFT);
        tx.setCurrentLevel(0);
        tx.setVersion(0L);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setUpdatedAt(LocalDateTime.now());
        return tx;
    }
}

package com.example.bankapproval.domain.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionApplication {
    private Long id;
    private String applicationNo;
    private String applicantId;
    private String applicantName;
    private String departmentId;
    private String mobileCiphertext;
    private String idCardCiphertext;
    private String accountCiphertext;
    private String counterpartyAccountCiphertext;
    private BigDecimal amount;
    private String currency;
    private String purpose;
    private Integer riskScore;
    private String riskLevel;
    private TransactionStatus status;
    private Integer currentLevel;
    private String rejectionReason;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

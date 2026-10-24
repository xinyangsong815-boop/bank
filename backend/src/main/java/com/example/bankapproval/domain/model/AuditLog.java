package com.example.bankapproval.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLog {
    private Long id;
    private Long transactionId;
    private String operatorId;
    private String operatorName;
    private String operation;
    private String detail;
    private String ipAddress;
    private LocalDateTime createdAt;
}

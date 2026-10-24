package com.example.bankapproval.domain.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApprovalRecord {
    private Long id;
    private Long transactionId;
    private Integer level;
    private String approverId;
    private String approverName;
    private String action;
    private String statusBefore;
    private String statusAfter;
    private String comment;
    private LocalDateTime createdAt;
}

package com.example.bankapproval.domain.repository;

import com.example.bankapproval.domain.model.TransactionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransactionQuery {
    private String keyword;
    private TransactionStatus status;
    private String applicantId;
    private String departmentId;
    private Integer currentLevel;
    private LocalDateTime startAt;
    private LocalDateTime endExclusive;
    private int offset;
    private int limit;
}

package com.example.bankapproval.domain.repository;

import com.example.bankapproval.domain.model.ApprovalRecord;

import java.util.List;

public interface ApprovalRecordRepository {
    void insert(ApprovalRecord record);

    List<ApprovalRecord> findByTransactionId(Long transactionId);
}

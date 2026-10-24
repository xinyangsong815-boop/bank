package com.example.bankapproval.domain.repository;

import com.example.bankapproval.domain.model.AuditLog;

import java.util.List;

public interface AuditLogRepository {
    void insert(AuditLog log);

    List<AuditLog> findByTransactionId(Long transactionId);
}

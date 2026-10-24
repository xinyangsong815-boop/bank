package com.example.bankapproval.domain.repository;

import com.example.bankapproval.domain.model.TransactionApplication;

import java.util.List;

/**
 * Repository 隔离领域层与 MyBatis，实现方式可替换而不影响业务服务。
 */
public interface TransactionRepository {
    void insert(TransactionApplication transaction);

    TransactionApplication findById(Long id);

    List<TransactionApplication> findPage(TransactionQuery query);

    long count(TransactionQuery query);

    int updateStatus(Long id, long expectedVersion, String expectedStatus, String nextStatus,
                     int nextLevel, String rejectionReason, int riskScore, String riskLevel);

    int updateForResubmit(TransactionApplication transaction, long expectedVersion, String expectedStatus);
}

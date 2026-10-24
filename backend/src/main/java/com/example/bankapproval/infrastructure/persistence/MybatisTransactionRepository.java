package com.example.bankapproval.infrastructure.persistence;

import com.example.bankapproval.domain.model.TransactionApplication;
import com.example.bankapproval.domain.repository.TransactionQuery;
import com.example.bankapproval.domain.repository.TransactionRepository;
import com.example.bankapproval.infrastructure.persistence.mapper.TransactionApplicationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisTransactionRepository implements TransactionRepository {
    private final TransactionApplicationMapper mapper;
    public MybatisTransactionRepository(TransactionApplicationMapper mapper) { this.mapper = mapper; }
    @Override public void insert(TransactionApplication transaction) { mapper.insert(transaction); }
    @Override public TransactionApplication findById(Long id) { return mapper.findById(id); }
    @Override public List<TransactionApplication> findPage(TransactionQuery query) { return mapper.findPage(query); }
    @Override public long count(TransactionQuery query) { return mapper.count(query); }
    @Override public int updateStatus(Long id, long expectedVersion, String expectedStatus, String nextStatus,
                                      int nextLevel, String rejectionReason, int riskScore, String riskLevel) {
        return mapper.updateStatus(id, expectedVersion, expectedStatus, nextStatus, nextLevel,
                rejectionReason, riskScore, riskLevel);
    }
    @Override public int updateForResubmit(TransactionApplication transaction, long expectedVersion, String expectedStatus) {
        return mapper.updateForResubmit(transaction, expectedVersion, expectedStatus);
    }
}

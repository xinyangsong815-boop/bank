package com.example.bankapproval.infrastructure.persistence;

import com.example.bankapproval.domain.model.ApprovalRecord;
import com.example.bankapproval.domain.repository.ApprovalRecordRepository;
import com.example.bankapproval.infrastructure.persistence.mapper.ApprovalRecordMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class MybatisApprovalRecordRepository implements ApprovalRecordRepository {
    private final ApprovalRecordMapper mapper;
    public MybatisApprovalRecordRepository(ApprovalRecordMapper mapper) { this.mapper = mapper; }
    @Override public void insert(ApprovalRecord record) { mapper.insert(record); }
    @Override public List<ApprovalRecord> findByTransactionId(Long id) { return mapper.findByTransactionId(id); }
}

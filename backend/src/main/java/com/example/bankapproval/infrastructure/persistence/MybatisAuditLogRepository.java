package com.example.bankapproval.infrastructure.persistence;

import com.example.bankapproval.domain.model.AuditLog;
import com.example.bankapproval.domain.repository.AuditLogRepository;
import com.example.bankapproval.infrastructure.persistence.mapper.AuditLogMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MybatisAuditLogRepository implements AuditLogRepository {
    private final AuditLogMapper mapper;

    public MybatisAuditLogRepository(AuditLogMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void insert(AuditLog log) {
        mapper.insert(log);
    }

    @Override
    public List<AuditLog> findByTransactionId(Long id) {
        return mapper.findByTransactionId(id);
    }
}

package com.example.bankapproval.infrastructure.persistence.mapper;

import com.example.bankapproval.domain.model.AuditLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuditLogMapper {
    void insert(AuditLog log);

    List<AuditLog> findByTransactionId(@Param("transactionId") Long transactionId);
}

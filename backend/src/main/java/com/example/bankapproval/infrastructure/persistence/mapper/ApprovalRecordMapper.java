package com.example.bankapproval.infrastructure.persistence.mapper;

import com.example.bankapproval.domain.model.ApprovalRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ApprovalRecordMapper {
    void insert(ApprovalRecord record);

    List<ApprovalRecord> findByTransactionId(@Param("transactionId") Long transactionId);
}

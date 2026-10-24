package com.example.bankapproval.infrastructure.persistence.mapper;

import com.example.bankapproval.domain.model.TransactionApplication;
import com.example.bankapproval.domain.repository.TransactionQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransactionApplicationMapper {
    void insert(TransactionApplication transaction);

    TransactionApplication findById(@Param("id") Long id);

    List<TransactionApplication> findPage(TransactionQuery query);

    long count(TransactionQuery query);

    int updateStatus(@Param("id") Long id,
                     @Param("expectedVersion") long expectedVersion,
                     @Param("expectedStatus") String expectedStatus,
                     @Param("nextStatus") String nextStatus,
                     @Param("nextLevel") int nextLevel,
                     @Param("rejectionReason") String rejectionReason,
                     @Param("riskScore") int riskScore,
                     @Param("riskLevel") String riskLevel);

    int updateForResubmit(@Param("tx") TransactionApplication transaction,
                          @Param("expectedVersion") long expectedVersion,
                          @Param("expectedStatus") String expectedStatus);
}

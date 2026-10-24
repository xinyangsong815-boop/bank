package com.example.bankapproval.api.dto;

import com.example.bankapproval.domain.model.TransactionStatus;
import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionResponse {
    private Long id;
    private String applicationNo;
    private String applicantId;
    private String applicantName;
    private String departmentId;
    private String mobileMasked;
    private String idCardMasked;
    private String accountMasked;
    private String counterpartyAccountMasked;
    // JSON 数字进入浏览器后可能丢失精度；金额明确作为十进制字符串返回。
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;
    private String currency;
    private String purpose;
    private Integer riskScore;
    private String riskLevel;
    private TransactionStatus status;
    private Integer currentLevel;
    private String rejectionReason;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

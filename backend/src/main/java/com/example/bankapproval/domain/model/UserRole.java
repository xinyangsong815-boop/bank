package com.example.bankapproval.domain.model;

import com.example.bankapproval.common.BizException;

public enum UserRole {
    APPLICANT(0), APPROVER_L1(1), APPROVER_L2(2), APPROVER_L3(3), APPROVER_L4(4), AUDITOR(0), ADMIN(0);

    private final int approvalLevel;
    UserRole(int approvalLevel) { this.approvalLevel = approvalLevel; }
    public int getApprovalLevel() { return approvalLevel; }

    public static UserRole parse(String value) {
        try {
            return UserRole.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BizException("INVALID_ROLE", "未知演示角色: " + value);
        }
    }
}

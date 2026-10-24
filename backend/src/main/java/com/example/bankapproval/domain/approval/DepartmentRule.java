package com.example.bankapproval.domain.approval;

import com.example.bankapproval.common.BizException;

public class DepartmentRule extends ApprovalRuleHandler {
    @Override
    protected void doCheck(ApprovalContext context) {
        if (!context.getTransaction().getDepartmentId().equals(context.getApprover().getDepartmentId())) {
            throw new BizException("DATA_SCOPE_DENIED", "只能审批本部门交易");
        }
    }
}

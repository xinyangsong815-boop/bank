package com.example.bankapproval.domain.approval;

import com.example.bankapproval.common.BizException;

public class RoleLevelRule extends ApprovalRuleHandler {
    @Override
    protected void doCheck(ApprovalContext context) {
        int actual = context.getApprover().getRole().getApprovalLevel();
        if (actual != context.getTransaction().getCurrentLevel()) {
            throw new BizException("APPROVAL_ROLE_MISMATCH", "当前角色不能执行该级审批");
        }
    }
}

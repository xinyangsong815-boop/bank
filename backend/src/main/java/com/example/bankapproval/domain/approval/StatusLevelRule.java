package com.example.bankapproval.domain.approval;

import com.example.bankapproval.common.BizException;

public class StatusLevelRule extends ApprovalRuleHandler {
    @Override
    protected void doCheck(ApprovalContext context) {
        String expected = "PENDING_L" + context.getTransaction().getCurrentLevel();
        if (!expected.equals(context.getTransaction().getStatus().name())) {
            throw new BizException("APPROVAL_LEVEL_MISMATCH", "交易不处于当前待审批级别");
        }
    }
}

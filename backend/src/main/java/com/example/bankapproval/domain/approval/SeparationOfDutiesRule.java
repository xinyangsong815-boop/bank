package com.example.bankapproval.domain.approval;

import com.example.bankapproval.common.BizException;

public class SeparationOfDutiesRule extends ApprovalRuleHandler {
    @Override protected void doCheck(ApprovalContext context) {
        if (context.getTransaction().getApplicantId().equals(context.getApprover().getUserId())) {
            throw new BizException("SEPARATION_OF_DUTIES", "申请人不能审批自己的交易");
        }
    }
}

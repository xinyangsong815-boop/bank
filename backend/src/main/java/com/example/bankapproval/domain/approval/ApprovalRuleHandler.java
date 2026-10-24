package com.example.bankapproval.domain.approval;

/** 责任链基类：每个处理器只校验一种审批规则。 */
public abstract class ApprovalRuleHandler {
    private ApprovalRuleHandler next;

    public ApprovalRuleHandler linkWith(ApprovalRuleHandler next) {
        this.next = next;
        return next;
    }

    public final void check(ApprovalContext context) {
        doCheck(context);
        if (next != null) next.check(context);
    }

    protected abstract void doCheck(ApprovalContext context);
}

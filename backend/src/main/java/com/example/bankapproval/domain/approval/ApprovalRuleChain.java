package com.example.bankapproval.domain.approval;

import org.springframework.stereotype.Component;

/** 责任链按“状态→角色→数据范围→职责分离”顺序快速失败。 */
@Component
public class ApprovalRuleChain {
    public void check(ApprovalContext context) {
        ApprovalRuleHandler head = new StatusLevelRule();
        head.linkWith(new RoleLevelRule())
                .linkWith(new DepartmentRule())
                .linkWith(new SeparationOfDutiesRule());
        head.check(context);
    }
}

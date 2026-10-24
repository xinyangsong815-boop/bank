package com.example.bankapproval.domain.approval;

import com.example.bankapproval.common.BizException;
import com.example.bankapproval.domain.model.TransactionApplication;
import com.example.bankapproval.domain.model.TransactionStatus;
import com.example.bankapproval.domain.model.UserRole;
import com.example.bankapproval.security.DemoPrincipal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ApprovalRuleChainTest {
    @Test
    void roleDepartmentAndSeparationOfDutiesAreChecked() {
        TransactionApplication tx = new TransactionApplication();
        tx.setStatus(TransactionStatus.PENDING_L2);
        tx.setCurrentLevel(2);
        tx.setDepartmentId("D001");
        tx.setApplicantId("U1001");
        ApprovalRuleChain chain = new ApprovalRuleChain();
        assertDoesNotThrow(() -> chain.check(new ApprovalContext(tx,
                new DemoPrincipal("A2002", "二级审批员", "D001", UserRole.APPROVER_L2))));
        assertThrows(BizException.class, () -> chain.check(new ApprovalContext(tx,
                new DemoPrincipal("A2001", "一级审批员", "D001", UserRole.APPROVER_L1))));
        assertThrows(BizException.class, () -> chain.check(new ApprovalContext(tx,
                new DemoPrincipal("A2002", "外部门审批员", "D002", UserRole.APPROVER_L2))));
    }
}

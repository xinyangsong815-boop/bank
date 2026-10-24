package com.example.bankapproval.domain.state;

import com.example.bankapproval.common.BizException;
import com.example.bankapproval.domain.model.TransactionStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionStateMachineTest {
    private final TransactionStateMachine machine = new TransactionStateMachine();

    @Test
    void shouldCompleteFourLevelApproval() {
        StateTransition first = machine.transit(TransactionStatus.DRAFT, TransactionEvent.SUBMIT);
        assertEquals(TransactionStatus.PENDING_L1, first.getNextStatus());
        assertEquals(TransactionStatus.PENDING_L2,
                machine.transit(first.getNextStatus(), TransactionEvent.APPROVE).getNextStatus());
        assertEquals(TransactionStatus.PENDING_L3,
                machine.transit(TransactionStatus.PENDING_L2, TransactionEvent.APPROVE).getNextStatus());
        assertEquals(TransactionStatus.PENDING_L4,
                machine.transit(TransactionStatus.PENDING_L3, TransactionEvent.APPROVE).getNextStatus());
        assertEquals(TransactionStatus.APPROVED,
                machine.transit(TransactionStatus.PENDING_L4, TransactionEvent.APPROVE).getNextStatus());
    }

    @Test
    void rejectedTransactionCanOnlyResubmit() {
        assertEquals(TransactionStatus.PENDING_L1,
                machine.transit(TransactionStatus.REJECTED, TransactionEvent.RESUBMIT).getNextStatus());
        assertThrows(BizException.class,
                () -> machine.transit(TransactionStatus.REJECTED, TransactionEvent.APPROVE));
    }
}

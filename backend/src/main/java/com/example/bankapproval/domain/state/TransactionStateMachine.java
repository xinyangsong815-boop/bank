package com.example.bankapproval.domain.state;

import com.example.bankapproval.common.BizException;
import com.example.bankapproval.domain.model.TransactionStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 状态模式：每一种状态只负责自己的合法迁移，避免 Service 中出现难以维护的巨大 if/else。
 * 新增“撤回”等动作时，可在对应状态类中局部扩展。
 */
@Component
public class TransactionStateMachine {
    private final Map<TransactionStatus, TransactionState> states = new EnumMap<TransactionStatus, TransactionState>(TransactionStatus.class);

    public TransactionStateMachine() {
        states.put(TransactionStatus.DRAFT, new DraftState());
        states.put(TransactionStatus.REJECTED, new RejectedState());
        states.put(TransactionStatus.PENDING_L1, new PendingState(1, TransactionStatus.PENDING_L2));
        states.put(TransactionStatus.PENDING_L2, new PendingState(2, TransactionStatus.PENDING_L3));
        states.put(TransactionStatus.PENDING_L3, new PendingState(3, TransactionStatus.PENDING_L4));
        states.put(TransactionStatus.PENDING_L4, new PendingState(4, TransactionStatus.APPROVED));
        states.put(TransactionStatus.APPROVED, new TerminalState());
    }

    public StateTransition transit(TransactionStatus status, TransactionEvent event) {
        return states.get(status).transit(event);
    }

    private abstract static class BaseState implements TransactionState {
        protected BizException illegal(TransactionEvent event) {
            return new BizException("ILLEGAL_STATE", "当前状态不允许操作: " + event);
        }
    }

    private static class DraftState extends BaseState {
        @Override public StateTransition transit(TransactionEvent event) {
            if (event == TransactionEvent.SUBMIT) return new StateTransition(TransactionStatus.PENDING_L1, 1);
            throw illegal(event);
        }
    }

    private static class RejectedState extends BaseState {
        @Override public StateTransition transit(TransactionEvent event) {
            if (event == TransactionEvent.RESUBMIT) return new StateTransition(TransactionStatus.PENDING_L1, 1);
            throw illegal(event);
        }
    }

    private static class PendingState extends BaseState {
        private final int level;
        private final TransactionStatus approvedNext;
        PendingState(int level, TransactionStatus approvedNext) {
            this.level = level;
            this.approvedNext = approvedNext;
        }
        @Override public StateTransition transit(TransactionEvent event) {
            if (event == TransactionEvent.APPROVE) {
                return new StateTransition(approvedNext, approvedNext == TransactionStatus.APPROVED ? 0 : level + 1);
            }
            if (event == TransactionEvent.REJECT) return new StateTransition(TransactionStatus.REJECTED, 0);
            throw illegal(event);
        }
    }

    private static class TerminalState extends BaseState {
        @Override public StateTransition transit(TransactionEvent event) { throw illegal(event); }
    }
}

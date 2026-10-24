package com.example.bankapproval.domain.state;

public interface TransactionState {
    StateTransition transit(TransactionEvent event);
}

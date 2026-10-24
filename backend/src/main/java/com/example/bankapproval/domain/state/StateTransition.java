package com.example.bankapproval.domain.state;

import com.example.bankapproval.domain.model.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StateTransition {
    private final TransactionStatus nextStatus;
    private final int nextLevel;
}

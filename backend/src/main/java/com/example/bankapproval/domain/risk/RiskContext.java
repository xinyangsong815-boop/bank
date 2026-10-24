package com.example.bankapproval.domain.risk;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class RiskContext {
    private final BigDecimal amount;
    private final String purpose;
    private final String counterpartyAccount;
}

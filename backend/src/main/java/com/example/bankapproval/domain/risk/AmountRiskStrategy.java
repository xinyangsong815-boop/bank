package com.example.bankapproval.domain.risk;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Order(10)
public class AmountRiskStrategy implements RiskStrategy {
    @Override public int score(RiskContext context) {
        BigDecimal amount = context.getAmount();
        if (amount.compareTo(new BigDecimal("1000000")) >= 0) return 55;
        if (amount.compareTo(new BigDecimal("100000")) >= 0) return 30;
        if (amount.compareTo(new BigDecimal("10000")) >= 0) return 10;
        return 0;
    }
    @Override public String name() { return "交易金额策略"; }
}

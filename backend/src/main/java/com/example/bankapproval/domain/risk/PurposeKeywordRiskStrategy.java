package com.example.bankapproval.domain.risk;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(20)
public class PurposeKeywordRiskStrategy implements RiskStrategy {
    @Override public int score(RiskContext context) {
        String purpose = context.getPurpose() == null ? "" : context.getPurpose().toLowerCase();
        return Arrays.asList("博彩", "代付", "虚拟币", "gambling", "crypto").stream().anyMatch(purpose::contains) ? 35 : 0;
    }
    @Override public String name() { return "用途关键词策略"; }
}

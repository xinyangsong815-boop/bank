package com.example.bankapproval.infrastructure.risk;

import com.example.bankapproval.domain.risk.RiskContext;
import com.example.bankapproval.domain.risk.RiskStrategy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 适配器模式：把遗留 SDK 转换成统一风险策略，无需污染风险引擎。 */
@Component
@Order(30)
public class LegacyRiskAdapter implements RiskStrategy {
    private final LegacyFraudClient client = new LegacyFraudClient();
    @Override public int score(RiskContext context) { return client.legacyPoints(context.getCounterpartyAccount()); }
    @Override public String name() { return "遗留黑名单适配器"; }
}

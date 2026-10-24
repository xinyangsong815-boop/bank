package com.example.bankapproval.domain.risk;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 策略模式：Spring 自动注入全部 RiskStrategy，增加新规则无需修改本类。
 */
@Component
public class RiskEngine {
    private final List<RiskStrategy> strategies;

    public RiskEngine(List<RiskStrategy> strategies) {
        this.strategies = strategies;
    }

    public RiskResult evaluate(RiskContext context) {
        int score = 0;
        for (RiskStrategy strategy : strategies) score += strategy.score(context);
        score = Math.min(score, 100);
        String level = score >= 70 ? "HIGH" : (score >= 30 ? "MEDIUM" : "LOW");
        return new RiskResult(score, level);
    }
}

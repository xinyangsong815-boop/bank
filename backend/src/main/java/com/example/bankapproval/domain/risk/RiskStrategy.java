package com.example.bankapproval.domain.risk;

public interface RiskStrategy {
    int score(RiskContext context);

    String name();
}

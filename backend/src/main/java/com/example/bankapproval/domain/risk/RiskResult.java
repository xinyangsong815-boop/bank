package com.example.bankapproval.domain.risk;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RiskResult {
    private final int score;
    private final String level;
}

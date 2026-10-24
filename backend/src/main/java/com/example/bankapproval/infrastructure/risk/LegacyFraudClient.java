package com.example.bankapproval.infrastructure.risk;

/**
 * 假设这是不可修改的旧风控 SDK，它的接口与新系统的 RiskStrategy 不兼容。
 */
public class LegacyFraudClient {
    public int legacyPoints(String account) {
        if (account == null) return 0;
        return account.endsWith("0000") || account.endsWith("9999") ? 25 : 0;
    }
}

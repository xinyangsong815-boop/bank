package com.example.bankapproval.config;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/** 生产环境安全闸：防止误把可伪造身份的教学认证或明文模式带入生产。 */
@Component
@Profile("prod")
public class ProductionSafetyGuard {
    private final BankSecurityProperties security;
    private final BankCryptoProperties crypto;

    public ProductionSafetyGuard(BankSecurityProperties security, BankCryptoProperties crypto) {
        this.security = security;
        this.crypto = crypto;
    }

    @PostConstruct
    public void verify() {
        if (security.isDemoAuthEnabled()) {
            throw new IllegalStateException("生产环境禁止启用请求头演示认证，请接入银行 IAM/OIDC");
        }
        if (!crypto.isEnabled()) {
            throw new IllegalStateException("生产环境禁止关闭敏感字段加密");
        }
    }
}

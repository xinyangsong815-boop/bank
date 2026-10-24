package com.example.bankapproval.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

/**
 * 使用 @ConfigurationProperties 集中管理加密配置，避免业务类到处读取环境变量。
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "bank.crypto")
public class BankCryptoProperties {
    private boolean enabled = true;

    /**
     * Base64 编码的 256 位 AES 密钥。生产环境必须来自 KMS/密钥托管服务。
     */
    @NotBlank
    private String base64Key;
}

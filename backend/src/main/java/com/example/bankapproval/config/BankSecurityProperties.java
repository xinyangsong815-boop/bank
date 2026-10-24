package com.example.bankapproval.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bank.security")
public class BankSecurityProperties {
    /** 仅教学演示。生产环境必须关闭并接入银行统一身份认证。 */
    private boolean demoAuthEnabled = true;
}

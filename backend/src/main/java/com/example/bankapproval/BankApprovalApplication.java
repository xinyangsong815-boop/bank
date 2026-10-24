package com.example.bankapproval;

import com.example.bankapproval.config.BankCryptoProperties;
import com.example.bankapproval.config.BankSecurityProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

// 本例使用显式的演示过滤器，不创建 Spring 默认用户名/随机密码，避免误解为真正登录。
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.example.bankapproval.infrastructure.persistence.mapper")
@EnableConfigurationProperties({BankCryptoProperties.class, BankSecurityProperties.class})
public class BankApprovalApplication {
    public static void main(String[] args) {
        SpringApplication.run(BankApprovalApplication.class, args);
    }
}

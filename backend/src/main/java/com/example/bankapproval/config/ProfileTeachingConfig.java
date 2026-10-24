package com.example.bankapproval.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 演示 @Profile：不同环境暴露一个只读的环境标识，不把环境判断散落在业务代码中。
 */
@Configuration
public class ProfileTeachingConfig {
    @Bean
    @Profile("dev")
    public RuntimeEnvironment devEnvironment() {
        return new RuntimeEnvironment("dev", false);
    }

    @Bean
    @Profile("prod")
    public RuntimeEnvironment prodEnvironment() {
        return new RuntimeEnvironment("prod", true);
    }

    @Bean
    @Profile("test")
    public RuntimeEnvironment testEnvironment() {
        return new RuntimeEnvironment("test", false);
    }

    public static class RuntimeEnvironment {
        private final String name;
        private final boolean production;

        public RuntimeEnvironment(String name, boolean production) {
            this.name = name;
            this.production = production;
        }

        public String getName() {
            return name;
        }

        public boolean isProduction() {
            return production;
        }
    }
}

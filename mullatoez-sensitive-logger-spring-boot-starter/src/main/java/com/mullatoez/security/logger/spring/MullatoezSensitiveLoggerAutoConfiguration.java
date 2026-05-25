package com.mullatoez.security.logger.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mullatoez.security.logger.core.SensitiveObjectMasker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(MullatoezSensitiveLoggerProperties.class)
public class MullatoezSensitiveLoggerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SensitiveObjectMasker sensitiveObjectMasker() {
        return new SensitiveObjectMasker();
    }

     @Bean
    @ConditionalOnMissingBean
    public ObjectMapper mullatoezSensitiveLoggerObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    public MullatoezSensitiveLogger mullatoezSensitiveLogger(
            SensitiveObjectMasker sensitiveObjectMasker,
            ObjectMapper objectMapper,
            MullatoezSensitiveLoggerProperties properties
    ) {
        return new MullatoezSensitiveLogger(
                sensitiveObjectMasker,
                objectMapper,
                properties
        );
    }
}
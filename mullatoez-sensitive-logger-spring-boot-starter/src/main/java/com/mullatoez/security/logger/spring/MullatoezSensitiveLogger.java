package com.mullatoez.security.logger.spring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mullatoez.security.logger.core.SensitiveObjectMasker;

public class MullatoezSensitiveLogger {

    private final SensitiveObjectMasker sensitiveObjectMasker;
    private final ObjectMapper objectMapper;
    private final MullatoezSensitiveLoggerProperties properties;

    public MullatoezSensitiveLogger(
            SensitiveObjectMasker sensitiveObjectMasker,
            ObjectMapper objectMapper,
            MullatoezSensitiveLoggerProperties properties
    ) {
        this.sensitiveObjectMasker = sensitiveObjectMasker;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String mask(Object source) {
        return mask(source, properties.isMaskFullByDefault());
    }

    public String mask(Object source, boolean maskFull) {
        if (!properties.isEnabled()) {
            return safeToString(source);
        }

        Object masked = sensitiveObjectMasker.mask(source, maskFull);

        if (!properties.isOutputJson()) {
            return safeToString(masked);
        }

        try {
            return objectMapper.writeValueAsString(masked);
        } catch (JsonProcessingException ex) {
            return safeToString(masked);
        }
    }

    private String safeToString(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }
}
package com.mullatoez.security.logger.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mullatoez.sensitive-logger")
public class MullatoezSensitiveLoggerProperties {

    /**
     * Enables or disables masking when using MullatoezSensitiveLogger.
     */
    private boolean enabled = true;

    /**
     * If true, @Sensitive fields are fully masked by default.
     * If false, @Sensitive fields are partially masked by default.
     */
    private boolean maskFullByDefault = false;

    /**
     * If true, masked log output is rendered as JSON.
     */
    private boolean outputJson = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMaskFullByDefault() {
        return maskFullByDefault;
    }

    public void setMaskFullByDefault(boolean maskFullByDefault) {
        this.maskFullByDefault = maskFullByDefault;
    }

    public boolean isOutputJson() {
        return outputJson;
    }

    public void setOutputJson(boolean outputJson) {
        this.outputJson = outputJson;
    }
}
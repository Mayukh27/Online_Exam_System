package com.examportal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FeatureFlags - Binds the "features" block in application.yml.
 * Set a flag to false to disable that module cleanly at runtime.
 * AI, PDF, and Email are false by default until external services are wired.
 */
@Component
@ConfigurationProperties(prefix = "features")
@Data
public class FeatureFlags {
    private boolean admin = true;
    private boolean teacher = true;
    private boolean analytics = true;
    private boolean blueprint = true;
    private boolean proctor = true;
    /** AI answer analysis - set true after LLM API is configured */
    private boolean ai = false;
    /** PDF report generation - set true after iText dependency verified */
    private boolean pdf = false;
    /** Email delivery - set true after SMTP credentials configured */
    private boolean email = false;
}

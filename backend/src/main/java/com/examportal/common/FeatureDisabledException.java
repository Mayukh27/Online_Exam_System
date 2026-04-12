package com.examportal.common;

/**
 * FeatureDisabledException - Thrown when a request targets a module that has been
 * disabled via feature flags. Returns HTTP 503 via GlobalExceptionHandler.
 */
public class FeatureDisabledException extends RuntimeException {
    public FeatureDisabledException(String feature) {
        super("Feature '" + feature + "' is currently disabled. Enable it in application.yml under features." + feature);
    }
}

package com.taxi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration class to enable Spring's scheduled task execution
 * 
 * This enables the @Scheduled annotation to work in the application
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // No additional configuration needed
    // Just enabling scheduling capability
}
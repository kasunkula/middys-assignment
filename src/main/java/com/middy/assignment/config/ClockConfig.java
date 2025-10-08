package com.middy.assignment.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Configuration class for providing a {@link java.time.Clock} bean.
 * <p>
 * This bean can be injected wherever a clock is needed for time-based operations,
 * allowing for easier testing and consistent time management across the application.
 */
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}

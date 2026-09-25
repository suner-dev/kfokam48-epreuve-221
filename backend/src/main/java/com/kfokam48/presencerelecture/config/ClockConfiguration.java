package com.kfokam48.presencerelecture.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {
    @Bean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }
}

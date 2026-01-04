package com.parthpanchal.springdemo.demo.config;

import com.parthpanchal.springdemo.demo.CircuitBreaker;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.circuitbreaker")
public class AppConfig {

    // Default values - will be overridden by application.yml if present
    private int failureThreshold = 5;
    private long rollingWindowMillis = 10000;
    private long slidingWindowSize = 5000;

    @PostConstruct
    public void init() {
        System.out.println("AppConfig initialized with values from yml/config: failureThreshold=" + failureThreshold + 
                          ", rollingWindowMillis=" + rollingWindowMillis + 
                          ", slidingWindowSize=" + slidingWindowSize);
    }

    @Bean
    public CircuitBreaker circuitBreaker() {
        System.out.println("Creating CircuitBreaker bean with values: failureThreshold=" + failureThreshold + 
                          ", rollingWindowMillis=" + rollingWindowMillis + 
                          ", slidingWindowSize=" + slidingWindowSize);
        return new CircuitBreaker(this);
    }
}

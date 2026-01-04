package com.parthpanchal.springdemo.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import com.parthpanchal.springdemo.demo.config.AppConfig;

@SpringBootApplication
@EnableConfigurationProperties(AppConfig.class)
public class DemoApplication {

    @Autowired
    CircuitBreakerDemo circuitBreakerDemo;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doSomethingAfterStartup() {
        circuitBreakerDemo.execute();
    }

}

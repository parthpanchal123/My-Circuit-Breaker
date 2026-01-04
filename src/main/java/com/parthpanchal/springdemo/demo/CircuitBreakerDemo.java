package com.parthpanchal.springdemo.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class CircuitBreakerDemo {

    @Autowired
    CircuitBreaker circuitBreaker;

    public void execute() {

        // Simulated unreliable client
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<String> unreliableCall = () -> {
            int n = counter.incrementAndGet();
            if (n % 4 != 0) { // fail 75% of the time
                throw new RuntimeException("remote failure " + n);
            }
            return "remote-success-" + n;
        };


        // Simple fallback (cached) value
        Supplier<String> fallback = () -> "cached-value";

        // Repeated calls to demonstrate behavior
        for (int i = 0; i < 30; i++) {
            String result = circuitBreaker.execute(unreliableCall, fallback);
            System.out.println("call " + i + " -> " + result + " (state=" + circuitBreaker.getState() + ")");
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }

    }
}

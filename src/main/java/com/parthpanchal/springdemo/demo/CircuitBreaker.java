package com.parthpanchal.springdemo.demo;

import com.parthpanchal.springdemo.demo.config.AppConfig;
import com.parthpanchal.springdemo.demo.enums.State;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class CircuitBreaker {

    private volatile State state = State.CLOSED;

    private final int failureThreshold;
    private final long rollingWindowMillis;
    private final long openStateMillis;

    private final ConcurrentLinkedDeque<Long> failureTimestamps = new ConcurrentLinkedDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    private final AtomicLong openUntil = new AtomicLong(0);
    private final AtomicBoolean halfOpenTrialInProgress = new AtomicBoolean(false);

    public CircuitBreaker(final AppConfig appConfig) {
        this.failureThreshold = appConfig.getFailureThreshold();
        this.rollingWindowMillis = appConfig.getRollingWindowMillis();
        this.openStateMillis = appConfig.getSlidingWindowSize();
        System.out.println("CircuitBreaker initialized: failureThreshold=" + failureThreshold + 
                          ", rollingWindowMillis=" + rollingWindowMillis + 
                          ", openStateMillis=" + openStateMillis);
    }

    public <T> T execute(Supplier<T> call, Supplier<T> fallback) {
        long now = Instant.now().toEpochMilli();

        if (state == State.OPEN) {
            if (now < openUntil.get()) {
                return fallback.get();
            } else {
                lock.lock();
                try {
                    if (state == State.OPEN && now >= openUntil.get()) {
                        System.out.println("Timeout elapsed! Transitioning from OPEN to HALF_OPEN");
                        state = State.HALF_OPEN;
                        halfOpenTrialInProgress.set(false);
                    }
                } finally {
                    lock.unlock();
                }
            }
        }

        if (state == State.HALF_OPEN) {
            if (!halfOpenTrialInProgress.compareAndSet(false, true)) {
                return fallback.get();
            }
            try {
                System.out.println("HALF_OPEN: Making test call to check if service recovered...");
                T result = call.get();
                lock.lock();
                try {
                    System.out.println("HALF_OPEN test succeeded! Transitioning back to CLOSED");
                    reset();
                    state = State.CLOSED;
                } finally {
                    lock.unlock();
                }
                return result;
            } catch (Throwable ex) {
                lock.lock();
                try {
                    System.out.println("HALF_OPEN test failed! Transitioning back to OPEN");
                    transitionToOpen(now);
                } finally {
                    lock.unlock();
                }
                return fallback.get();
            } finally {
                halfOpenTrialInProgress.set(false);
            }
        }

        // state == CLOSED
        try {
            T result = call.get();
            pruneOldFailures(now);
            return result;
        } catch (Throwable ex) {
            lock.lock();
            try {
                recordFailure(now);
            } finally {
                lock.unlock();
            }
            return fallback.get();
        }
    }

    private void recordFailure(long timestamp) {
        failureTimestamps.addLast(timestamp);
        pruneOldFailures(timestamp);
        System.out.println("Failure recorded. Current failures: " + failureTimestamps.size() + "/" + failureThreshold);
        if (failureTimestamps.size() >= failureThreshold) {
            System.out.println("Threshold reached! Transitioning to OPEN");
            transitionToOpen(timestamp);
        }
    }

    private void pruneOldFailures(long now) {
        long cutoff = now - rollingWindowMillis;
        int beforeSize = failureTimestamps.size();
        while (true) {
            Long t = failureTimestamps.peekFirst();
            if (t == null || t >= cutoff) break;
            failureTimestamps.pollFirst();
        }
        int afterSize = failureTimestamps.size();
        if (beforeSize != afterSize) {
            System.out.println("Pruned " + (beforeSize - afterSize) + " old failures. Window: " + rollingWindowMillis + "ms, Cutoff: " + cutoff + ", Now: " + now);
        }
    }

    private void transitionToOpen(long now) {
        state = State.OPEN;
        openUntil.set(now + openStateMillis);
        failureTimestamps.clear();
        System.out.println("Circuit opened! Will stay open until: " + openUntil.get() + " (current: " + now + ")");
    }

    private void reset() {
        failureTimestamps.clear();
        openUntil.set(0);
    }

    public String getState() {
        return this.state.name();
    }

}

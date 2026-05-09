package com.minitoon.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * Retry Utilities - Generic retry mechanism with exponential backoff
 */
@Slf4j
@Component
public class RetryUtils {

    /**
     * Execute with retry logic
     */
    public <T> T executeWithRetry(Supplier<T> operation, int maxRetries, long baseDelayMs, String operationName) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxRetries) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt >= maxRetries) {
                    break;
                }

                long delay = baseDelayMs * (1L << attempt); // Exponential backoff
                log.warn("{} failed (attempt {}/{}), retrying in {}ms: {}", 
                        operationName, attempt, maxRetries, delay, e.getMessage());

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        throw new RuntimeException(operationName + " failed after " + maxRetries + " attempts", lastException);
    }

    /**
     * Execute with retry (void operation)
     */
    public void executeWithRetry(Runnable operation, int maxRetries, long baseDelayMs, String operationName) {
        executeWithRetry(() -> {
            operation.run();
            return null;
        }, maxRetries, baseDelayMs, operationName);
    }
}

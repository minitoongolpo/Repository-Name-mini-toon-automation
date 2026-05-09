package com.minitoon.exception;

/**
 * Exception for AI service failures
 */
public class AiServiceException extends MiniToonException {

    private final String serviceName;
    private final int retryCount;

    public AiServiceException(String serviceName, String message, int retryCount) {
        super(String.format("[%s] %s (retry: %d)", serviceName, message, retryCount));
        this.serviceName = serviceName;
        this.retryCount = retryCount;
    }

    public AiServiceException(String serviceName, String message, Throwable cause, int retryCount) {
        super(String.format("[%s] %s (retry: %d)", serviceName, message, retryCount), cause);
        this.serviceName = serviceName;
        this.retryCount = retryCount;
    }

    public String getServiceName() { return serviceName; }
    public int getRetryCount() { return retryCount; }
}

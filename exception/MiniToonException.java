package com.minitoon.exception;

/**
 * Base exception for MiniToon application
 */
public class MiniToonException extends RuntimeException {

    public MiniToonException(String message) {
        super(message);
    }

    public MiniToonException(String message, Throwable cause) {
        super(message, cause);
    }
}

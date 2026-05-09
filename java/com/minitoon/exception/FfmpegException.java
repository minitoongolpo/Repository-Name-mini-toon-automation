package com.minitoon.exception;

/**
 * Exception for FFmpeg processing failures
 */
public class FfmpegException extends MiniToonException {

    private final int exitCode;
    private final String command;

    public FfmpegException(String message, String command, int exitCode) {
        super(String.format("FFmpeg failed (exit: %d): %s [Command: %s]", exitCode, message, command));
        this.exitCode = exitCode;
        this.command = command;
    }

    public int getExitCode() { return exitCode; }
    public String getCommand() { return command; }
}

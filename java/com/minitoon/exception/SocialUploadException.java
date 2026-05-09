package com.minitoon.exception;

/**
 * Exception for social media upload failures
 */
public class SocialUploadException extends MiniToonException {

    private final String platform;
    private final String videoId;

    public SocialUploadException(String platform, String videoId, String message) {
        super(String.format("[%s] Upload failed for video %s: %s", platform, videoId, message));
        this.platform = platform;
        this.videoId = videoId;
    }

    public SocialUploadException(String platform, String videoId, String message, Throwable cause) {
        super(String.format("[%s] Upload failed for video %s: %s", platform, videoId, message), cause);
        this.platform = platform;
        this.videoId = videoId;
    }

    public String getPlatform() { return platform; }
    public String getVideoId() { return videoId; }
}

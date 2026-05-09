package com.minitoon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Application Properties - Centralized configuration
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String name;
    private String version;
    private AiProperties ai;
    private FfmpegProperties ffmpeg;
    private VideoProperties video;
    private SocialProperties social;
    private SchedulerProperties scheduler;
    private ContentProperties content;

    @Data
    public static class AiProperties {
        private GeminiProperties gemini;
        private LeonardoProperties leonardo;
        private ElevenLabsProperties elevenlabs;
    }

    @Data
    public static class GeminiProperties {
        private String apiKey;
        private String model;
        private String baseUrl;
        private int timeout;
        private int maxRetries;
    }

    @Data
    public static class LeonardoProperties {
        private String apiKey;
        private String baseUrl;
        private int timeout;
        private int maxRetries;
        private String defaultModel;
    }

    @Data
    public static class ElevenLabsProperties {
        private String apiKey;
        private String baseUrl;
        private int timeout;
        private int maxRetries;
        private String voiceId;
        private String modelId;
    }

    @Data
    public static class FfmpegProperties {
        private String executable;
        private String ffprobe;
        private String workingDir;
        private String outputDir;
        private String tempDir;
    }

    @Data
    public static class VideoProperties {
        private int width;
        private int height;
        private int fps;
        private int durationSeconds;
        private String bitrate;
        private String audioBitrate;
        private String codec;
        private String pixelFormat;
        private String preset;
    }

    @Data
    public static class SocialProperties {
        private YoutubeProperties youtube;
        private InstagramProperties instagram;
        private FacebookProperties facebook;
    }

    @Data
    public static class YoutubeProperties {
        private boolean enabled;
        private String clientId;
        private String clientSecret;
        private String refreshToken;
        private String channelId;
        private String privacyStatus;
        private String categoryId;
    }

    @Data
    public static class InstagramProperties {
        private boolean enabled;
        private String accessToken;
        private String accountId;
    }

    @Data
    public static class FacebookProperties {
        private boolean enabled;
        private String accessToken;
        private String pageId;
    }

    @Data
    public static class SchedulerProperties {
        private boolean enabled;
        private String dailyGenerationTime;
        private String dailyUploadTime;
        private String timezone;
    }

    @Data
    public static class ContentProperties {
        private String language;
        private String targetAudience;
        private List<String> storyThemes;
        private int maxScenes;
        private int minScenes;
    }
}

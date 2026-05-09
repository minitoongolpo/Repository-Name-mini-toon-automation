package com.minitoon.controller;

import com.minitoon.config.AppProperties;
import com.minitoon.dto.ApiResponse;
import com.minitoon.service.social.FacebookService;
import com.minitoon.service.social.InstagramService;
import com.minitoon.service.social.YouTubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Health & Status Controller - System monitoring endpoints
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/status")
@RequiredArgsConstructor
public class HealthController {

    private final AppProperties appProperties;
    private final YouTubeService youTubeService;
    private final InstagramService instagramService;
    private final FacebookService facebookService;

    /**
     * System health check
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", appProperties.getName());
        health.put("version", appProperties.getVersion());
        health.put("scheduler", appProperties.getScheduler().isEnabled());

        return ResponseEntity.ok(ApiResponse.success(health, "System is healthy"));
    }

    /**
     * Service configuration status
     */
    @GetMapping("/services")
    public ResponseEntity<ApiResponse<Map<String, Object>>> serviceStatus() {
        Map<String, Object> services = new HashMap<>();

        // AI Services
        Map<String, Object> ai = new HashMap<>();
        ai.put("gemini", appProperties.getAi().getGemini().getApiKey() != null && 
                !appProperties.getAi().getGemini().getApiKey().isEmpty());
        ai.put("leonardo", appProperties.getAi().getLeonardo().getApiKey() != null && 
                !appProperties.getAi().getLeonardo().getApiKey().isEmpty());
        ai.put("elevenlabs", appProperties.getAi().getElevenlabs().getApiKey() != null && 
                !appProperties.getAi().getElevenlabs().getApiKey().isEmpty());
        services.put("ai", ai);

        // Social Media
        Map<String, Object> social = new HashMap<>();
        social.put("youtube", youTubeService.isConfigured());
        social.put("instagram", instagramService.isConfigured());
        social.put("facebook", facebookService.isConfigured());
        services.put("social", social);

        // FFmpeg
        Map<String, Object> ffmpeg = new HashMap<>();
        ffmpeg.put("available", isFfmpegAvailable());
        services.put("ffmpeg", ffmpeg);

        return ResponseEntity.ok(ApiResponse.success(services, "Service status retrieved"));
    }

    private boolean isFfmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    appProperties.getFfmpeg().getExecutable(), "-version");
            Process process = pb.start();
            return process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}

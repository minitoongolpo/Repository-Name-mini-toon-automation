package com.minitoon.service.social;

import com.minitoon.config.AppProperties;
import com.minitoon.exception.SocialUploadException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Facebook Service - Uploads videos to Facebook Page
 * Uses Graph API for page video publishing
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookService {

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v22.0";

    /**
     * Upload video to Facebook Page
     */
    public String uploadPageVideo(String videoPath, String title, String description) {
        log.info("Uploading Facebook Page video: {}", title);

        if (!appProperties.getSocial().getFacebook().isEnabled()) {
            log.warn("Facebook upload is disabled");
            return null;
        }

        try {
            String pageId = appProperties.getSocial().getFacebook().getPageId();
            String accessToken = appProperties.getSocial().getFacebook().getAccessToken();

            // For cloud deployment, video must be publicly accessible URL
            String videoUrl = getPublicVideoUrl(videoPath);

            String url = String.format("%s/%s/videos", GRAPH_API_BASE, pageId);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("file_url", videoUrl);
            requestBody.put("title", title);
            requestBody.put("description", description);
            requestBody.put("access_token", accessToken);

            JsonNode response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                            .filter(throwable -> {
                                log.warn("Facebook upload failed, retrying: {}", throwable.getMessage());
                                return true;
                            }))
                    .block(Duration.ofSeconds(120));

            if (response == null) {
                throw new SocialUploadException("Facebook", videoPath, "Empty response", null);
            }

            if (response.has("error")) {
                JsonNode error = response.path("error");
                throw new SocialUploadException("Facebook", videoPath, 
                        "API Error: " + error.path("message").asText(), null);
            }

            String videoId = response.path("id").asText();
            log.info("Facebook video uploaded successfully. Video ID: {}", videoId);
            return videoId;

        } catch (SocialUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new SocialUploadException("Facebook", videoPath, "Upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * For cloud deployment, video must be publicly accessible.
     */
    private String getPublicVideoUrl(String localPath) {
        // TODO: Implement file upload to public CDN/S3 and return URL
        return localPath;
    }

    public boolean isConfigured() {
        AppProperties.SocialProperties.FacebookProperties fb = appProperties.getSocial().getFacebook();
        return fb.isEnabled() 
                && fb.getAccessToken() != null && !fb.getAccessToken().isEmpty()
                && fb.getPageId() != null && !fb.getPageId().isEmpty();
    }
}

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
import java.util.Base64;

/**
 * Instagram Service - Uploads Reels via Graph API
 * Uses container-based upload flow: create container -> poll status -> publish
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstagramService {

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String GRAPH_API_BASE = "https://graph.facebook.com/v22.0";
    private static final int MAX_POLL_ATTEMPTS = 10;
    private static final int POLL_INTERVAL_MS = 30000; // 30 seconds

    /**
     * Upload video as Instagram Reel
     */
    public String uploadReel(String videoPath, String caption, String coverImagePath) {
        log.info("Uploading Instagram Reel");

        if (!appProperties.getSocial().getInstagram().isEnabled()) {
            log.warn("Instagram upload is disabled");
            return null;
        }

        try {
            String containerId = createReelContainer(videoPath, caption, coverImagePath);
            waitForContainerReady(containerId);
            String mediaId = publishReel(containerId);

            log.info("Instagram Reel published. Media ID: {}", mediaId);
            return mediaId;

        } catch (Exception e) {
            throw new SocialUploadException("Instagram", videoPath, "Upload failed: " + e.getMessage(), e);
        }
    }

    private String createReelContainer(String videoPath, String caption, String coverImagePath) {
        String accountId = appProperties.getSocial().getInstagram().getAccountId();
        String accessToken = appProperties.getSocial().getInstagram().getAccessToken();

        try {
            String videoUrl = getPublicVideoUrl(videoPath);
            String url = String.format("%s/%s/media", GRAPH_API_BASE, accountId);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("media_type", "REELS");
            requestBody.put("video_url", videoUrl);
            requestBody.put("caption", caption);
            requestBody.put("share_to_feed", true);

            if (coverImagePath != null) {
                requestBody.put("cover_url", getPublicImageUrl(coverImagePath));
            }

            JsonNode response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))
                    .block(Duration.ofSeconds(60));

            if (response == null || !response.has("id")) {
                throw new SocialUploadException("Instagram", videoPath, "Failed to create container", null);
            }

            return response.path("id").asText();

        } catch (Exception e) {
            throw new SocialUploadException("Instagram", videoPath, "Container failed: " + e.getMessage(), e);
        }
    }

    private void waitForContainerReady(String containerId) {
        String accessToken = appProperties.getSocial().getInstagram().getAccessToken();

        for (int attempt = 0; attempt < MAX_POLL_ATTEMPTS; attempt++) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);

                String url = String.format("%s/%s?fields=status_code&access_token=%s", 
                        GRAPH_API_BASE, containerId, accessToken);

                JsonNode response = webClientBuilder.build()
                        .get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block(Duration.ofSeconds(30));

                if (response == null) continue;

                String status = response.path("status_code").asText("UNKNOWN");
                log.info("Container status: {} (attempt {}/{})", status, attempt + 1, MAX_POLL_ATTEMPTS);

                if ("FINISHED".equals(status)) {
                    return;
                } else if ("ERROR".equals(status)) {
                    throw new SocialUploadException("Instagram", containerId, "Container processing failed", null);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SocialUploadException("Instagram", containerId, "Polling interrupted", null);
            } catch (Exception e) {
                log.warn("Poll attempt {} failed: {}", attempt + 1, e.getMessage());
            }
        }

        throw new SocialUploadException("Instagram", containerId, "Container polling timed out", null);
    }

    private String publishReel(String containerId) {
        String accountId = appProperties.getSocial().getInstagram().getAccountId();
        String accessToken = appProperties.getSocial().getInstagram().getAccessToken();

        try {
            String url = String.format("%s/%s/media_publish", GRAPH_API_BASE, accountId);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("creation_id", containerId);
            requestBody.put("access_token", accessToken);

            JsonNode response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(2, Duration.ofSeconds(10)))
                    .block(Duration.ofSeconds(60));

            if (response == null || !response.has("id")) {
                throw new SocialUploadException("Instagram", containerId, "Publish failed", null);
            }

            return response.path("id").asText();

        } catch (Exception e) {
            throw new SocialUploadException("Instagram", containerId, "Publish failed: " + e.getMessage(), e);
        }
    }

    /**
     * For cloud deployment, video must be publicly accessible.
     * Implement file hosting or use a temporary public URL service.
     */
    private String getPublicVideoUrl(String localPath) {
        // TODO: Implement file upload to public CDN/S3 and return URL
        // For now, assumes video is already publicly accessible
        return localPath;
    }

    private String getPublicImageUrl(String localPath) {
        // TODO: Implement file upload to public CDN/S3 and return URL
        return localPath;
    }

    public boolean isConfigured() {
        AppProperties.SocialProperties.InstagramProperties ig = appProperties.getSocial().getInstagram();
        return ig.isEnabled() 
                && ig.getAccessToken() != null && !ig.getAccessToken().isEmpty()
                && ig.getAccountId() != null && !ig.getAccountId().isEmpty();
    }
}

package com.minitoon.service.ai;

import com.minitoon.config.AppProperties;
import com.minitoon.exception.AiServiceException;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Leonardo AI Service - Generates cartoon scenes and thumbnails
 * Uses Leonardo AI API for high-quality cartoon/cinematic image generation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeonardoService {

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String GENERATIONS_ENDPOINT = "/generations";
    private static final String GENERATION_ENDPOINT = "/generations/";

    /**
     * Generate a single image for a scene
     */
    public String generateSceneImage(String prompt, String outputPath) {
        log.info("Generating scene image with prompt length: {}", prompt.length());

        String generationId = submitGeneration(prompt, "SCENE");
        String imageUrl = pollForResult(generationId);

        if (imageUrl != null) {
            downloadImage(imageUrl, outputPath);
            log.info("Scene image saved to: {}", outputPath);
            return outputPath;
        }

        throw new AiServiceException("Leonardo", "Failed to generate scene image", 0);
    }

    /**
     * Generate multiple scene images in batch
     */
    public List<String> generateSceneImages(List<String> prompts, String baseOutputDir) {
        log.info("Generating {} scene images", prompts.size());
        List<String> paths = new ArrayList<>();

        for (int i = 0; i < prompts.size(); i++) {
            String outputPath = baseOutputDir + "/scene_" + (i + 1) + ".png";
            try {
                String path = generateSceneImage(prompts.get(i), outputPath);
                paths.add(path);
                Thread.sleep(2000);
            } catch (Exception e) {
                log.error("Failed to generate scene {}: {}", i + 1, e.getMessage());
                throw new AiServiceException("Leonardo", "Scene " + (i + 1) + " generation failed", e, i);
            }
        }

        return paths;
    }

    /**
     * Generate thumbnail image
     */
    public String generateThumbnail(String prompt, String outputPath) {
        log.info("Generating thumbnail");

        String enhancedPrompt = prompt + ", YouTube thumbnail style, eye-catching, " +
                "bright colors, cartoon characters, vertical format, high contrast, " +
                "expressive faces, kids-friendly, professional design";

        String generationId = submitGeneration(enhancedPrompt, "THUMBNAIL");
        String imageUrl = pollForResult(generationId);

        if (imageUrl != null) {
            downloadImage(imageUrl, outputPath);
            log.info("Thumbnail saved to: {}", outputPath);
            return outputPath;
        }

        throw new AiServiceException("Leonardo", "Failed to generate thumbnail", 0);
    }

    private String submitGeneration(String prompt, String type) {
        AppProperties.LeonardoProperties leonardo = appProperties.getAi().getLeonardo();

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("prompt", prompt);
            requestBody.put("modelId", leonardo.getDefaultModel());
            requestBody.put("width", type.equals("THUMBNAIL") ? 720 : 720);
            requestBody.put("height", type.equals("THUMBNAIL") ? 1280 : 1280);
            requestBody.put("num_images", 1);
            requestBody.put("guidance_scale", 7);
            requestBody.put("scheduler", "EULER_DISCRETE");
            requestBody.put("presetStyle", type.equals("THUMBNAIL") ? "DYNAMIC" : "CINEMATIC");

            ObjectNode alchemy = requestBody.putObject("alchemy");
            alchemy.put("enabled", true);
            alchemy.put("photoReal", false);
            alchemy.put("highResolution", true);

            JsonNode response = webClientBuilder.build()
                    .post()
                    .uri(leonardo.getBaseUrl() + GENERATIONS_ENDPOINT)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .header("Authorization", "Bearer " + leonardo.getApiKey())
                    .header("Accept", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(leonardo.getMaxRetries(), Duration.ofSeconds(3))
                            .filter(throwable -> {
                                log.warn("Leonardo API submit failed, retrying: {}", throwable.getMessage());
                                return true;
                            }))
                    .block(Duration.ofSeconds(leonardo.getTimeout()));

            if (response == null || !response.has("sdGenerationJob")) {
                throw new AiServiceException("Leonardo", "Invalid submission response", 0);
            }

            String generationId = response.path("sdGenerationJob").path("generationId").asText();
            log.info("Generation submitted with ID: {}", generationId);
            return generationId;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Leonardo", "Submit failed: " + e.getMessage(), e, 0);
        }
    }

    private String pollForResult(String generationId) {
        AppProperties.LeonardoProperties leonardo = appProperties.getAi().getLeonardo();
        int maxAttempts = 30;
        int attempt = 0;

        while (attempt < maxAttempts) {
            try {
                Thread.sleep(2000);

                JsonNode response = webClientBuilder.build()
                        .get()
                        .uri(leonardo.getBaseUrl() + GENERATION_ENDPOINT + generationId)
                        .header("Authorization", "Bearer " + leonardo.getApiKey())
                        .header("Accept", "application/json")
                        .retrieve()
                        .bodyToMono(JsonNode.class)
                        .block(Duration.ofSeconds(30));

                if (response == null || !response.has("generations_by_pk")) {
                    attempt++;
                    continue;
                }

                JsonNode generation = response.path("generations_by_pk");
                String status = generation.path("status").asText("PENDING");

                if ("COMPLETE".equals(status)) {
                    JsonNode images = generation.path("generated_images");
                    if (images.isArray() && images.size() > 0) {
                        String url = images.get(0).path("url").asText();
                        log.info("Generation complete, image URL obtained");
                        return url;
                    }
                } else if ("FAILED".equals(status)) {
                    throw new AiServiceException("Leonardo", "Generation failed on server", 0);
                }

                attempt++;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiServiceException("Leonardo", "Polling interrupted", 0);
            } catch (Exception e) {
                log.warn("Poll attempt {} failed: {}", attempt, e.getMessage());
                attempt++;
            }
        }

        throw new AiServiceException("Leonardo", "Generation timed out after " + maxAttempts + " attempts", 0);
    }

    private void downloadImage(String imageUrl, String outputPath) {
        try {
            Path path = Path.of(outputPath);
            Files.createDirectories(path.getParent());

            try (InputStream in = new URL(imageUrl).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Image downloaded: {} ({} bytes)", outputPath, Files.size(path));
        } catch (IOException e) {
            throw new AiServiceException("Leonardo", "Failed to download image: " + e.getMessage(), e, 0);
        }
    }
}

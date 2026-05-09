package com.minitoon.service.ai;

import com.minitoon.config.AppProperties;
import com.minitoon.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

/**
 * Gemini AI Service - Generates Bengali stories, scenes, prompts, and metadata
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final WebClient.Builder webClientBuilder;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    private static final String GEMINI_ENDPOINT = "/models/%s:generateContent";

    /**
     * Generate a Bengali story for kids with emotional/funny storytelling
     */
    public String generateBengaliStory(String theme, String targetAudience) {
        log.info("Generating Bengali story with theme: {} for audience: {}", theme, targetAudience);

        String prompt = buildStoryPrompt(theme, targetAudience);
        String response = callGemini(prompt);

        log.info("Successfully generated story of length: {} characters", response.length());
        return response;
    }

    /**
     * Generate scene descriptions and prompts from a story
     */
    public SceneGenerationResult generateScenes(String story, int sceneCount) {
        log.info("Generating {} scenes from story", sceneCount);

        String prompt = buildScenePrompt(story, sceneCount);
        String response = callGemini(prompt);

        return parseSceneResult(response);
    }

    /**
     * Generate video title, description, and hashtags
     */
    public MetadataResult generateMetadata(String story, String theme) {
        log.info("Generating metadata for story");

        String prompt = buildMetadataPrompt(story, theme);
        String response = callGemini(prompt);

        return parseMetadataResult(response);
    }

    /**
     * Generate thumbnail prompt for Leonardo AI
     */
    public String generateThumbnailPrompt(String story, String theme) {
        log.info("Generating thumbnail prompt");

        String prompt = buildThumbnailPrompt(story, theme);
        return callGemini(prompt);
    }

    /**
     * Generate image prompts for each scene (optimized for Leonardo AI)
     */
    public List<String> generateImagePrompts(List<String> sceneTexts, String theme) {
        log.info("Generating image prompts for {} scenes", sceneTexts.size());

        StringBuilder sb = new StringBuilder();
        sb.append("Generate detailed image generation prompts in English for each Bengali scene below. ");
        sb.append("Each prompt should be optimized for AI image generation (Leonardo AI). ");
        sb.append("Style: Cute cartoon, vibrant colors, kids-friendly, emotional storytelling. ");
        sb.append("Format: Return ONLY a numbered list (1. prompt, 2. prompt, etc.).\n\n");
        sb.append("Theme: ").append(theme).append("\n\n");

        for (int i = 0; i < sceneTexts.size(); i++) {
            sb.append("Scene ").append(i + 1).append(": ").append(sceneTexts.get(i)).append("\n");
        }

        String response = callGemini(sb.toString());
        return parseNumberedList(response);
    }

    // Private helper methods

    private String callGemini(String prompt) {
        AppProperties.GeminiProperties gemini = appProperties.getAi().getGemini();

        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = requestBody.putArray("contents");
            ObjectNode content = contents.addObject();
            ArrayNode parts = content.putArray("parts");
            ObjectNode part = parts.addObject();
            part.put("text", prompt);

            // Safety settings
            ArrayNode safetySettings = requestBody.putArray("safetySettings");
            String[] categories = {"HARM_CATEGORY_HARASSMENT", "HARM_CATEGORY_HATE_SPEECH", 
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT", "HARM_CATEGORY_DANGEROUS_CONTENT"};
            for (String category : categories) {
                ObjectNode safety = safetySettings.addObject();
                safety.put("category", category);
                safety.put("threshold", "BLOCK_NONE");
            }

            // Generation config
            ObjectNode generationConfig = requestBody.putObject("generationConfig");
            generationConfig.put("temperature", 0.8);
            generationConfig.put("topK", 40);
            generationConfig.put("topP", 0.95);
            generationConfig.put("maxOutputTokens", 2048);

            String endpoint = String.format(GEMINI_ENDPOINT, gemini.getModel());
            String url = gemini.getBaseUrl() + endpoint + "?key=" + gemini.getApiKey();

            JsonNode response = webClientBuilder.build()
                    .post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .retryWhen(Retry.backoff(gemini.getMaxRetries(), Duration.ofSeconds(2))
                            .filter(throwable -> {
                                log.warn("Gemini API call failed, retrying: {}", throwable.getMessage());
                                return true;
                            }))
                    .block(Duration.ofSeconds(gemini.getTimeout()));

            if (response == null) {
                throw new AiServiceException("Gemini", "Empty response from API", 0);
            }

            // Extract text from response
            JsonNode candidates = response.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode candidate = candidates.get(0);
                JsonNode contentNode = candidate.path("content");
                JsonNode partsNode = contentNode.path("parts");
                if (partsNode.isArray() && partsNode.size() > 0) {
                    return partsNode.get(0).path("text").asText().trim();
                }
            }

            throw new AiServiceException("Gemini", "Could not parse response", 0);

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceException("Gemini", "API call failed: " + e.getMessage(), e, 0);
        }
    }

    private String buildStoryPrompt(String theme, String targetAudience) {
        return String.format("""
            Create a heartwarming, emotional, and slightly funny Bengali story for %s.
            Theme: %s

            Requirements:
            - Language: Bengali (বাংলা)
            - Length: Approximately 200-250 words
            - Style: Simple, engaging, with emotional depth
            - Characters: Relatable, lovable characters
            - Moral: Subtle life lesson
            - Tone: Warm, friendly, entertaining
            - Include dialogue between characters
            - Add a surprising or funny twist

            Return ONLY the story text in Bengali, no explanations.
            """, targetAudience, theme);
    }

    private String buildScenePrompt(String story, int sceneCount) {
        return String.format("""
            Break down this Bengali story into %d visual scenes for a short video.

            Story:
            %s

            For each scene, provide:
            1. Scene description in Bengali (what happens visually)
            2. Narration text in Bengali (what the voiceover says)
            3. Duration in seconds (total should be 30-45 seconds)

            Format:
            SCENE 1:
            Description: [Bengali visual description]
            Narration: [Bengali narration text]
            Duration: [seconds]

            SCENE 2:
            ...

            Return in structured format as shown above.
            """, sceneCount, story);
    }

    private String buildMetadataPrompt(String story, String theme) {
        return String.format("""
            Generate YouTube Shorts metadata for this Bengali story:

            Story: %s
            Theme: %s

            Provide:
            1. Catchy title in Bengali (max 100 chars, include emojis)
            2. Engaging description in Bengali (2-3 sentences + call to action)
            3. Hashtags (mix of Bengali and English, 10-15 hashtags)

            Format:
            TITLE: [title]
            DESCRIPTION: [description]
            HASHTAGS: [#hashtag1 #hashtag2 ...]
            """, story, theme);
    }

    private String buildThumbnailPrompt(String story, String theme) {
        return String.format("""
            Create a detailed English image generation prompt for a YouTube Shorts thumbnail.

            Story theme: %s
            Story summary: %s

            Requirements:
            - Eye-catching, colorful cartoon style
            - Expressive characters with big emotions
            - Bright, vibrant background
            - Kids-friendly design
            - Vertical format optimized
            - Include text overlay suggestion

            Return ONLY the image prompt (English).
            """, theme, story.substring(0, Math.min(story.length(), 200)));
    }

    private SceneGenerationResult parseSceneResult(String response) {
        List<String> descriptions = new java.util.ArrayList<>();
        List<String> narrations = new java.util.ArrayList<>();
        List<Integer> durations = new java.util.ArrayList<>();

        String[] lines = response.split("\n");
        StringBuilder currentDesc = new StringBuilder();
        StringBuilder currentNarr = new StringBuilder();
        int currentDuration = 7;

        for (String line : lines) {
            line = line.trim();
            if (line.toUpperCase().startsWith("SCENE")) {
                if (currentDesc.length() > 0) {
                    descriptions.add(currentDesc.toString().trim());
                    narrations.add(currentNarr.toString().trim());
                    durations.add(currentDuration);
                }
                currentDesc = new StringBuilder();
                currentNarr = new StringBuilder();
            } else if (line.toLowerCase().startsWith("description:")) {
                currentDesc.append(line.substring("description:".length()).trim());
            } else if (line.toLowerCase().startsWith("narration:")) {
                currentNarr.append(line.substring("narration:".length()).trim());
            } else if (line.toLowerCase().startsWith("duration:")) {
                try {
                    currentDuration = Integer.parseInt(line.replaceAll("[^0-9]", ""));
                } catch (NumberFormatException ignored) {}
            } else if (currentDesc.length() > 0 && !line.isEmpty()) {
                currentDesc.append(" ").append(line);
            } else if (currentNarr.length() > 0 && !line.isEmpty()) {
                currentNarr.append(" ").append(line);
            }
        }

        if (currentDesc.length() > 0) {
            descriptions.add(currentDesc.toString().trim());
            narrations.add(currentNarr.toString().trim());
            durations.add(currentDuration);
        }

        return new SceneGenerationResult(descriptions, narrations, durations);
    }

    private MetadataResult parseMetadataResult(String response) {
        String title = "";
        String description = "";
        String hashtags = "";

        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.toUpperCase().startsWith("TITLE:")) {
                title = line.substring("TITLE:".length()).trim();
            } else if (line.toUpperCase().startsWith("DESCRIPTION:")) {
                description = line.substring("DESCRIPTION:".length()).trim();
            } else if (line.toUpperCase().startsWith("HASHTAGS:")) {
                hashtags = line.substring("HASHTAGS:".length()).trim();
            }
        }

        return new MetadataResult(title, description, hashtags);
    }

    private List<String> parseNumberedList(String response) {
        List<String> items = new java.util.ArrayList<>();
        String[] lines = response.split("\n");

        for (String line : lines) {
            line = line.trim();
            // Match patterns like "1. text" or "1) text"
            if (line.matches("^\d+[.\)]\s*.+")) {
                String item = line.replaceFirst("^\d+[.\)]\s*", "").trim();
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
        }

        return items;
    }

    // Result records
    public record SceneGenerationResult(
            List<String> descriptions,
            List<String> narrations,
            List<Integer> durations
    ) {}

    public record MetadataResult(
            String title,
            String description,
            String hashtags
    ) {}
}

package com.minitoon.service;

import com.minitoon.config.AppProperties;
import com.minitoon.exception.AiServiceException;
import com.minitoon.exception.FfmpegException;
import com.minitoon.model.*;
import com.minitoon.repository.SceneRepository;
import com.minitoon.repository.VideoProjectRepository;
import com.minitoon.service.ai.ElevenLabsService;
import com.minitoon.service.ai.GeminiService;
import com.minitoon.service.ai.LeonardoService;
import com.minitoon.service.ffmpeg.FfmpegService;
import com.minitoon.utils.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Video Generation Service - Orchestrates the complete AI video creation pipeline
 * 
 * Workflow:
 * 1. Generate Bengali story with Gemini
 * 2. Break into scenes with Gemini
 * 3. Generate images with Leonardo AI
 * 4. Generate narration with ElevenLabs
 * 5. Build video with FFmpeg
 * 6. Generate thumbnail
 * 7. Generate metadata (title, description, hashtags)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoGenerationService {

    private final GeminiService geminiService;
    private final LeonardoService leonardoService;
    private final ElevenLabsService elevenLabsService;
    private final FfmpegService ffmpegService;
    private final VideoProjectRepository projectRepository;
    private final SceneRepository sceneRepository;
    private final AppProperties appProperties;
    private final FileUtils fileUtils;

    private final Random random = new Random();

    /**
     * Generate a complete video project from scratch
     */
    @Async("aiGenerationExecutor")
    @Transactional
    public VideoProject generateVideo(String theme, String targetAudience, int sceneCount) {
        log.info("Starting video generation for theme: {}", theme);

        // Create project
        VideoProject project = VideoProject.builder()
                .title("Auto-generated " + theme + " story")
                .targetLanguage("bn")
                .targetAudience(targetAudience)
                .status(ProjectStatus.CREATED)
                .storyTheme(theme)
                .build();

        project = projectRepository.save(project);
        String projectDir = appProperties.getFfmpeg().getWorkingDir() + "/" + project.getId();

        try {
            // Step 1: Generate story
            project.setStatus(ProjectStatus.STORY_GENERATED);
            String story = geminiService.generateBengaliStory(theme, targetAudience);
            project.setStory(story);
            project = projectRepository.save(project);
            log.info("Story generated ({} chars)", story.length());

            // Step 2: Generate scenes
            project.setStatus(ProjectStatus.SCENES_GENERATED);
            GeminiService.SceneGenerationResult scenesResult = geminiService.generateScenes(story, sceneCount);
            List<Scene> scenes = createScenes(project, scenesResult);
            project.setScenes(scenes);
            project = projectRepository.save(project);
            log.info("{} scenes generated", scenes.size());

            // Step 3: Generate image prompts and images
            project.setStatus(ProjectStatus.IMAGES_GENERATED);
            List<String> sceneTexts = scenes.stream().map(Scene::getSceneText).toList();
            List<String> imagePrompts = geminiService.generateImagePrompts(sceneTexts, theme);

            List<String> imagePaths = new ArrayList<>();
            for (int i = 0; i < scenes.size() && i < imagePrompts.size(); i++) {
                String imagePath = projectDir + "/scene_" + (i + 1) + ".png";
                leonardoService.generateSceneImage(imagePrompts.get(i), imagePath);
                imagePaths.add(imagePath);

                Scene scene = scenes.get(i);
                scene.setImagePrompt(imagePrompts.get(i));
                scene.setLocalImagePath(imagePath);
                scene.setStatus(SceneStatus.IMAGE_GENERATED);
                sceneRepository.save(scene);
            }
            project = projectRepository.save(project);
            log.info("{} images generated", imagePaths.size());

            // Step 4: Generate narration
            project.setStatus(ProjectStatus.NARRATION_GENERATED);
            List<String> narrations = scenesResult.narrations();
            String narrationPath = projectDir + "/narration.mp3";

            // Combine all narrations
            StringBuilder fullNarration = new StringBuilder();
            for (int i = 0; i < narrations.size(); i++) {
                fullNarration.append(narrations.get(i));
                if (i < narrations.size() - 1) {
                    fullNarration.append(". ");
                }
            }

            elevenLabsService.generateKidsNarration(fullNarration.toString(), narrationPath);
            project.setNarrationAudioPath(narrationPath);

            // Update scene narration paths
            for (Scene scene : scenes) {
                scene.setNarrationAudioPath(narrationPath);
                scene.setStatus(SceneStatus.NARRATION_GENERATED);
                sceneRepository.save(scene);
            }
            project = projectRepository.save(project);
            log.info("Narration generated");

            // Step 5: Generate subtitles
            String subtitlePath = projectDir + "/subtitles.srt";
            List<Integer> durations = scenesResult.durations();
            ffmpegService.generateSubtitles(narrations, durations, subtitlePath);
            project.setSubtitlePath(subtitlePath);
            project = projectRepository.save(project);

            // Step 6: Build video with FFmpeg
            project.setStatus(ProjectStatus.VIDEO_RENDERING);
            String videoPath = projectDir + "/final_video.mp4";
            ffmpegService.buildVideo(imagePaths, narrationPath, subtitlePath, durations, videoPath);
            project.setFinalVideoPath(videoPath);
            project.setStatus(ProjectStatus.VIDEO_RENDERED);

            // Get video duration
            double duration = ffmpegService.getVideoDuration(videoPath);
            project.setVideoDuration((int) duration);
            project = projectRepository.save(project);
            log.info("Video rendered: {} ({}s)", videoPath, duration);

            // Step 7: Generate thumbnail
            String thumbnailPath = projectDir + "/thumbnail.png";
            String thumbnailPrompt = geminiService.generateThumbnailPrompt(story, theme);
            leonardoService.generateThumbnail(thumbnailPrompt, thumbnailPath);
            project.setThumbnailPath(thumbnailPath);
            project.setStatus(ProjectStatus.THUMBNAIL_GENERATED);
            project = projectRepository.save(project);
            log.info("Thumbnail generated");

            // Step 8: Generate metadata
            GeminiService.MetadataResult metadata = geminiService.generateMetadata(story, theme);
            project.setGeneratedTitle(metadata.title());
            project.setGeneratedDescription(metadata.description());
            project.setGeneratedHashtags(metadata.hashtags());
            project.setStatus(ProjectStatus.METADATA_GENERATED);
            project.setStatus(ProjectStatus.READY_FOR_UPLOAD);
            project = projectRepository.save(project);
            log.info("Metadata generated: {}", metadata.title());

            log.info("Video generation complete for project: {}", project.getId());
            return project;

        } catch (Exception e) {
            log.error("Video generation failed for project {}: {}", project.getId(), e.getMessage(), e);
            project.setStatus(ProjectStatus.FAILED);
            project.setErrorMessage(e.getMessage());
            project.setRetryCount(project.getRetryCount() + 1);
            return projectRepository.save(project);
        }
    }

    /**
     * Generate video with random theme
     */
    @Async("aiGenerationExecutor")
    public VideoProject generateDailyVideo() {
        List<String> themes = appProperties.getContent().getStoryThemes();
        String theme = themes.get(random.nextInt(themes.size()));
        int sceneCount = appProperties.getContent().getMinScenes() + 
                random.nextInt(appProperties.getContent().getMaxScenes() - appProperties.getContent().getMinScenes() + 1);

        return generateVideo(theme, appProperties.getContent().getTargetAudience(), sceneCount);
    }

    private List<Scene> createScenes(VideoProject project, GeminiService.SceneGenerationResult result) {
        List<Scene> scenes = new ArrayList<>();
        List<String> descriptions = result.descriptions();
        List<String> narrations = result.narrations();
        List<Integer> durations = result.durations();

        for (int i = 0; i < descriptions.size(); i++) {
            Scene scene = Scene.builder()
                    .project(project)
                    .sceneNumber(i + 1)
                    .sceneText(descriptions.get(i))
                    .narrationText(narrations.get(i))
                    .durationSeconds(i < durations.size() ? durations.get(i) : 7)
                    .status(SceneStatus.PENDING)
                    .build();
            scenes.add(sceneRepository.save(scene));
        }

        return scenes;
    }
}

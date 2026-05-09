package com.minitoon.controller;

import com.minitoon.dto.ApiResponse;
import com.minitoon.dto.GenerateVideoRequest;
import com.minitoon.dto.VideoResponse;
import com.minitoon.model.ProjectStatus;
import com.minitoon.model.VideoProject;
import com.minitoon.repository.VideoProjectRepository;
import com.minitoon.service.SocialUploadService;
import com.minitoon.service.VideoGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Video Controller - REST API endpoints for video management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoGenerationService videoGenerationService;
    private final SocialUploadService socialUploadService;
    private final VideoProjectRepository projectRepository;

    /**
     * Generate a new video manually
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<VideoResponse>> generateVideo(
            @Valid @RequestBody GenerateVideoRequest request) {
        log.info("Manual video generation requested: {}", request.getTitle());

        VideoProject project = videoGenerationService.generateVideo(
                request.getStoryTheme() != null ? request.getStoryTheme() : "adventure",
                request.getTargetAudience(),
                request.getSceneCount()
        );

        return ResponseEntity.accepted()
                .body(ApiResponse.success(toResponse(project), "Video generation started"));
    }

    /**
     * Get video by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoResponse>> getVideo(@PathVariable String id) {
        VideoProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + id));

        return ResponseEntity.ok(ApiResponse.success(toResponse(project), "Video found"));
    }

    /**
     * Get all videos
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<VideoResponse>>> getAllVideos(
            @RequestParam(required = false) ProjectStatus status) {
        List<VideoProject> projects = status != null 
                ? projectRepository.findByStatus(status)
                : projectRepository.findAll();

        List<VideoResponse> responses = projects.stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses, 
                "Found " + responses.size() + " videos"));
    }

    /**
     * Upload video to social media manually
     */
    @PostMapping("/{id}/upload")
    public ResponseEntity<ApiResponse<String>> uploadVideo(@PathVariable String id) {
        log.info("Manual upload requested for video: {}", id);

        socialUploadService.uploadToAllPlatforms(id);

        return ResponseEntity.accepted()
                .body(ApiResponse.success(null, "Upload process started"));
    }

    /**
     * Retry failed video
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<ApiResponse<VideoResponse>> retryVideo(@PathVariable String id) {
        VideoProject project = projectRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Video not found: " + id));

        if (project.getStatus() == ProjectStatus.FAILED) {
            project.setStatus(ProjectStatus.CREATED);
            project.setRetryCount(project.getRetryCount() + 1);
            projectRepository.save(project);

            VideoProject newProject = videoGenerationService.generateVideo(
                    project.getStoryTheme(),
                    project.getTargetAudience(),
                    5
            );

            return ResponseEntity.accepted()
                    .body(ApiResponse.success(toResponse(newProject), "Retry started"));
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("Video is not in failed state", "/api/v1/videos/" + id + "/retry"));
    }

    private VideoResponse toResponse(VideoProject project) {
        return VideoResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .status(project.getStatus())
                .uploadStatus(project.getUploadStatus())
                .story(project.getStory())
                .generatedTitle(project.getGeneratedTitle())
                .generatedDescription(project.getGeneratedDescription())
                .generatedHashtags(project.getGeneratedHashtags())
                .finalVideoPath(project.getFinalVideoPath())
                .thumbnailPath(project.getThumbnailPath())
                .youtubeVideoId(project.getYoutubeVideoId())
                .instagramMediaId(project.getInstagramMediaId())
                .facebookVideoId(project.getFacebookVideoId())
                .videoDuration(project.getVideoDuration())
                .sceneCount(project.getScenes() != null ? project.getScenes().size() : 0)
                .createdAt(project.getCreatedAt())
                .scheduledUploadTime(project.getScheduledUploadTime())
                .actualUploadTime(project.getActualUploadTime())
                .errorMessage(project.getErrorMessage())
                .retryCount(project.getRetryCount())
                .build();
    }
}

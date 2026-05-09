package com.minitoon.service;

import com.minitoon.config.AppProperties;
import com.minitoon.model.ProjectStatus;
import com.minitoon.model.UploadStatus;
import com.minitoon.model.VideoProject;
import com.minitoon.repository.VideoProjectRepository;
import com.minitoon.service.social.FacebookService;
import com.minitoon.service.social.InstagramService;
import com.minitoon.service.social.YouTubeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Social Media Upload Service - Handles uploads to all platforms
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialUploadService {

    private final YouTubeService youTubeService;
    private final InstagramService instagramService;
    private final FacebookService facebookService;
    private final VideoProjectRepository projectRepository;

    @Async("uploadExecutor")
    @Transactional
    public void uploadToAllPlatforms(String projectId) {
        log.info("Starting social media upload for project: {}", projectId);

        VideoProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Project not found: " + projectId));

        if (project.getStatus() != ProjectStatus.READY_FOR_UPLOAD) {
            log.warn("Project {} not ready for upload. Status: {}", projectId, project.getStatus());
            return;
        }

        project.setStatus(ProjectStatus.UPLOADING);
        project.setUploadStatus(UploadStatus.PENDING);
        project = projectRepository.save(project);

        boolean youtubeSuccess = false;
        boolean instagramSuccess = false;
        boolean facebookSuccess = false;

        if (youTubeService.isConfigured()) {
            try {
                String youtubeId = youTubeService.uploadShorts(
                        project.getFinalVideoPath(),
                        project.getGeneratedTitle(),
                        project.getGeneratedDescription(),
                        project.getGeneratedHashtags(),
                        project.getThumbnailPath()
                );
                project.setYoutubeVideoId(youtubeId);
                youtubeSuccess = true;
                project.setUploadStatus(UploadStatus.YOUTUBE_UPLOADED);
                log.info("YouTube upload successful: {}", youtubeId);
            } catch (Exception e) {
                log.error("YouTube upload failed: {}", e.getMessage());
            }
        }

        if (instagramService.isConfigured()) {
            try {
                String instagramId = instagramService.uploadReel(
                        project.getFinalVideoPath(),
                        project.getGeneratedTitle() + "\n\n" + project.getGeneratedDescription() + "\n\n" + project.getGeneratedHashtags(),
                        project.getThumbnailPath()
                );
                project.setInstagramMediaId(instagramId);
                instagramSuccess = true;
                log.info("Instagram upload successful: {}", instagramId);
            } catch (Exception e) {
                log.error("Instagram upload failed: {}", e.getMessage());
            }
        }

        if (facebookService.isConfigured()) {
            try {
                String facebookId = facebookService.uploadPageVideo(
                        project.getFinalVideoPath(),
                        project.getGeneratedTitle(),
                        project.getGeneratedDescription() + "\n\n" + project.getGeneratedHashtags()
                );
                project.setFacebookVideoId(facebookId);
                facebookSuccess = true;
                log.info("Facebook upload successful: {}", facebookId);
            } catch (Exception e) {
                log.error("Facebook upload failed: {}", e.getMessage());
            }
        }

        if (youtubeSuccess && instagramSuccess && facebookSuccess) {
            project.setUploadStatus(UploadStatus.ALL_UPLOADED);
            project.setStatus(ProjectStatus.UPLOADED);
        } else if (youtubeSuccess || instagramSuccess || facebookSuccess) {
            project.setUploadStatus(UploadStatus.PARTIAL_FAILURE);
            project.setStatus(ProjectStatus.UPLOADED);
        } else {
            project.setUploadStatus(UploadStatus.FAILED);
            project.setStatus(ProjectStatus.FAILED);
            project.setErrorMessage("All platform uploads failed");
        }

        project.setActualUploadTime(LocalDateTime.now());
        projectRepository.save(project);

        log.info("Upload complete for project {}. Status: {}", projectId, project.getUploadStatus());
    }
}

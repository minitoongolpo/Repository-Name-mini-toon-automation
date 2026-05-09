package com.minitoon.service.scheduler;

import com.minitoon.config.AppProperties;
import com.minitoon.model.ProjectStatus;
import com.minitoon.model.VideoProject;
import com.minitoon.repository.VideoProjectRepository;
import com.minitoon.service.SocialUploadService;
import com.minitoon.service.VideoGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily Scheduler - Automates video generation and upload
 * Runs at configured times (default: 6 AM generation, 8 AM upload)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DailySchedulerService {

    private final VideoGenerationService videoGenerationService;
    private final SocialUploadService socialUploadService;
    private final VideoProjectRepository projectRepository;
    private final AppProperties appProperties;

    /**
     * Daily video generation at configured time (default 6:00 AM)
     * Cron: 0 0 6 * * * = Every day at 6:00 AM
     */
    @Scheduled(cron = "${app.scheduler.daily-generation-time:0 0 6 * * *}", 
               zone = "${app.scheduler.timezone:Asia/Dhaka}")
    public void dailyVideoGeneration() {
        if (!appProperties.getScheduler().isEnabled()) {
            log.info("Scheduler is disabled, skipping generation");
            return;
        }

        log.info("Starting daily video generation at {}", LocalDateTime.now());

        try {
            VideoProject project = videoGenerationService.generateDailyVideo();
            log.info("Daily video generation initiated. Project ID: {}", project.getId());
        } catch (Exception e) {
            log.error("Daily video generation failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Daily upload at configured time (default 8:00 AM)
     * Cron: 0 0 8 * * * = Every day at 8:00 AM
     */
    @Scheduled(cron = "${app.scheduler.daily-upload-time:0 0 8 * * *}", 
               zone = "${app.scheduler.timezone:Asia/Dhaka}")
    public void dailyUpload() {
        if (!appProperties.getScheduler().isEnabled()) {
            log.info("Scheduler is disabled, skipping upload");
            return;
        }

        log.info("Starting daily upload at {}", LocalDateTime.now());

        try {
            // Find videos ready for upload from yesterday
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            List<VideoProject> readyProjects = projectRepository
                    .findByStatusAndCreatedAtAfter(ProjectStatus.READY_FOR_UPLOAD, yesterday);

            log.info("Found {} projects ready for upload", readyProjects.size());

            for (VideoProject project : readyProjects) {
                try {
                    socialUploadService.uploadToAllPlatforms(project.getId());
                    log.info("Upload initiated for project: {}", project.getId());
                } catch (Exception e) {
                    log.error("Upload failed for project {}: {}", project.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Daily upload process failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Retry failed uploads every 2 hours
     */
    @Scheduled(cron = "0 0 */2 * * *", zone = "${app.scheduler.timezone:Asia/Dhaka}")
    public void retryFailedUploads() {
        if (!appProperties.getScheduler().isEnabled()) {
            return;
        }

        log.info("Checking for failed uploads to retry");

        try {
            List<VideoProject> failedProjects = projectRepository
                    .findByStatusAndRetryCountLessThan(ProjectStatus.FAILED, 3);

            for (VideoProject project : failedProjects) {
                if (project.getRetryCount() < 3) {
                    log.info("Retrying project {} (attempt {})", 
                            project.getId(), project.getRetryCount() + 1);
                    socialUploadService.uploadToAllPlatforms(project.getId());
                }
            }
        } catch (Exception e) {
            log.error("Retry process failed: {}", e.getMessage());
        }
    }

    /**
     * Cleanup old temp files weekly (Sundays at 3 AM)
     */
    @Scheduled(cron = "0 0 3 * * 0", zone = "${app.scheduler.timezone:Asia/Dhaka}")
    public void weeklyCleanup() {
        log.info("Running weekly cleanup");
        // Implement temp file cleanup logic
    }
}

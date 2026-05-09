package com.minitoon.dto;

import com.minitoon.model.ProjectStatus;
import com.minitoon.model.UploadStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for video project response
 */
@Data
@Builder
public class VideoResponse {
    private String id;
    private String title;
    private String description;
    private ProjectStatus status;
    private UploadStatus uploadStatus;
    private String story;
    private String generatedTitle;
    private String generatedDescription;
    private String generatedHashtags;
    private String finalVideoPath;
    private String thumbnailPath;
    private String youtubeVideoId;
    private String instagramMediaId;
    private String facebookVideoId;
    private Integer videoDuration;
    private Integer sceneCount;
    private LocalDateTime createdAt;
    private LocalDateTime scheduledUploadTime;
    private LocalDateTime actualUploadTime;
    private String errorMessage;
    private Integer retryCount;
}

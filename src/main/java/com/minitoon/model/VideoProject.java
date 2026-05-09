package com.minitoon.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Video Project Entity - Represents a complete video generation project
 */
@Entity
@Table(name = "video_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String story;

    @Column(name = "target_language", nullable = false)
    private String targetLanguage = "bn";

    @Column(name = "target_audience")
    private String targetAudience = "kids";

    @Enumerated(EnumType.STRING)
    @Column(name = "project_status", nullable = false)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status")
    @Builder.Default
    private UploadStatus uploadStatus = UploadStatus.PENDING;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("sceneNumber")
    private List<Scene> scenes;

    @Column(name = "narration_audio_path")
    private String narrationAudioPath;

    @Column(name = "background_music_path")
    private String backgroundMusicPath;

    @Column(name = "final_video_path")
    private String finalVideoPath;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    @Column(name = "subtitle_path")
    private String subtitlePath;

    @Column(name = "generated_title")
    private String generatedTitle;

    @Column(name = "generated_description", columnDefinition = "TEXT")
    private String generatedDescription;

    @Column(name = "generated_hashtags")
    private String generatedHashtags;

    @Column(name = "youtube_video_id")
    private String youtubeVideoId;

    @Column(name = "instagram_media_id")
    private String instagramMediaId;

    @Column(name = "facebook_video_id")
    private String facebookVideoId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "scheduled_upload_time")
    private LocalDateTime scheduledUploadTime;

    @Column(name = "actual_upload_time")
    private LocalDateTime actualUploadTime;

    @Column(name = "video_duration")
    private Integer videoDuration;

    @Column(name = "video_width")
    @Builder.Default
    private Integer videoWidth = 720;

    @Column(name = "video_height")
    @Builder.Default
    private Integer videoHeight = 1280;

    @Column(name = "story_theme")
    private String storyTheme;

    @Version
    private Long version;
}

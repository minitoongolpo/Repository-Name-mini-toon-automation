package com.minitoon.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Scene Entity - Represents individual scenes in a video
 */
@Entity
@Table(name = "scenes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scene {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private VideoProject project;

    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    @Column(name = "scene_text", columnDefinition = "TEXT")
    private String sceneText;

    @Column(name = "image_prompt", columnDefinition = "TEXT")
    private String imagePrompt;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "local_image_path")
    private String localImagePath;

    @Column(name = "narration_text")
    private String narrationText;

    @Column(name = "narration_audio_path")
    private String narrationAudioPath;

    @Column(name = "duration_seconds")
    @Builder.Default
    private Integer durationSeconds = 7;

    @Column(name = "transition_type")
    @Builder.Default
    private String transitionType = "fade";

    @Enumerated(EnumType.STRING)
    @Column(name = "scene_status")
    @Builder.Default
    private SceneStatus status = SceneStatus.PENDING;

    @Column(name = "error_message")
    private String errorMessage;
}

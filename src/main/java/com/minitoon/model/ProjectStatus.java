package com.minitoon.model;

/**
 * Project Status Enum - Tracks video generation lifecycle
 */
public enum ProjectStatus {
    CREATED,           // Project initialized
    STORY_GENERATED,   // AI story created
    SCENES_GENERATED,  // Scene prompts created
    IMAGES_GENERATED,  // Leonardo images generated
    NARRATION_GENERATED, // ElevenLabs audio generated
    VIDEO_RENDERING,   // FFmpeg processing
    VIDEO_RENDERED,    // Video complete
    THUMBNAIL_GENERATED, // Thumbnail ready
    METADATA_GENERATED, // Title/desc/hashtags ready
    READY_FOR_UPLOAD,  // All assets ready
    UPLOADING,         // Upload in progress
    UPLOADED,          // Successfully uploaded
    FAILED,            // Generation/upload failed
    RETRYING           // Automatic retry in progress
}

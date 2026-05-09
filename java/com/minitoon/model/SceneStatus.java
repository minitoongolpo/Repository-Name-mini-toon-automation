package com.minitoon.model;

/**
 * Scene Status Enum - Tracks individual scene processing
 */
public enum SceneStatus {
    PENDING,
    IMAGE_GENERATING,
    IMAGE_GENERATED,
    IMAGE_FAILED,
    NARRATION_GENERATING,
    NARRATION_GENERATED,
    NARRATION_FAILED,
    COMPLETE
}

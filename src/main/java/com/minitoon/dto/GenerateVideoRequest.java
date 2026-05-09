package com.minitoon.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * DTO for manual video generation request
 */
@Data
public class GenerateVideoRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private String storyTheme;

    @Min(value = 3, message = "Minimum 3 scenes required")
    @Max(value = 7, message = "Maximum 7 scenes allowed")
    private Integer sceneCount = 5;

    @Min(value = 20, message = "Minimum duration 20 seconds")
    @Max(value = 60, message = "Maximum duration 60 seconds")
    private Integer durationSeconds = 35;

    private String targetAudience = "kids";

    private List<String> customPrompts;

    private Boolean uploadToYoutube = true;
    private Boolean uploadToInstagram = true;
    private Boolean uploadToFacebook = true;
}

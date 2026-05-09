package com.minitoon.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * File Utilities - Helper methods for file operations
 */
@Slf4j
@Component
public class FileUtils {

    /**
     * Create directory if it doesn't exist
     */
    public void createDirectoryIfNotExists(String path) {
        try {
            Path dir = Paths.get(path);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.info("Created directory: {}", path);
            }
        } catch (IOException e) {
            log.error("Failed to create directory {}: {}", path, e.getMessage());
        }
    }

    /**
     * Delete file if exists
     */
    public void deleteIfExists(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", path, e.getMessage());
        }
    }

    /**
     * Get file extension
     */
    public String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    /**
     * Generate unique filename
     */
    public String generateUniqueFilename(String prefix, String extension) {
        return prefix + "_" + System.currentTimeMillis() + "." + extension;
    }
}

package com.minitoon.service.ffmpeg;

import com.minitoon.config.AppProperties;
import com.minitoon.exception.FfmpegException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FFmpeg Service - Video rendering engine
 * Combines images, audio, subtitles, transitions into vertical Shorts/Reels
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FfmpegService {

    private final AppProperties appProperties;

    /**
     * Build complete video from scenes with narration and subtitles
     */
    public String buildVideo(List<String> imagePaths, String audioPath, String subtitlePath,
                             List<Integer> durations, String outputPath) {
        log.info("Building video with {} scenes, audio: {}", imagePaths.size(), audioPath);

        try {
            // Step 1: Create individual scene videos with Ken Burns effect
            List<String> sceneVideos = new ArrayList<>();
            for (int i = 0; i < imagePaths.size(); i++) {
                String sceneVideo = createSceneVideo(imagePaths.get(i), durations.get(i), i);
                sceneVideos.add(sceneVideo);
            }

            // Step 2: Concatenate scenes with transitions
            String concatenatedVideo = concatenateScenes(sceneVideos, outputPath + ".tmp.mp4");

            // Step 3: Add audio and subtitles
            String finalVideo = addAudioAndSubtitles(concatenatedVideo, audioPath, subtitlePath, outputPath);

            // Cleanup temp files
            cleanupTempFiles(sceneVideos, concatenatedVideo);

            log.info("Video built successfully: {}", finalVideo);
            return finalVideo;

        } catch (Exception e) {
            throw new FfmpegException("Video build failed: " + e.getMessage(), "buildVideo", -1);
        }
    }

    /**
     * Create a single scene video with Ken Burns zoom effect
     */
    private String createSceneVideo(String imagePath, int duration, int index) {
        String output = appProperties.getFfmpeg().getTempDir() + "/scene_" + index + ".mp4";

        List<String> command = new ArrayList<>();
        command.add(appProperties.getFfmpeg().getExecutable());
        command.add("-y");
        command.add("-loop");
        command.add("1");
        command.add("-i");
        command.add(imagePath);
        command.add("-vf");
        // Ken Burns effect: slow zoom and pan
        command.add("scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2, " +
                "zoompan=z='min(zoom+0.0015,1.5)':d=" + (duration * 30) + ":s=720x1280:x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)', " +
                "fps=30,format=yuv420p");
        command.add("-c:v");
        command.add("libx264");
        command.add("-t");
        command.add(String.valueOf(duration));
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add("-preset");
        command.add("fast");
        command.add(output);

        executeCommand(command, "createSceneVideo_" + index);
        return output;
    }

    /**
     * Concatenate scene videos with fade transitions
     */
    private String concatenateScenes(List<String> sceneVideos, String outputPath) {
        // Create concat list file
        String listFile = appProperties.getFfmpeg().getTempDir() + "/concat_list.txt";
        StringBuilder listContent = new StringBuilder();
        for (String video : sceneVideos) {
            listContent.append("file '").append(video).append("'
");
        }
        try {
            Files.writeString(Path.of(listFile), listContent.toString());
        } catch (Exception e) {
            throw new FfmpegException("Failed to create concat list", "concatenateScenes", -1);
        }

        List<String> command = new ArrayList<>();
        command.add(appProperties.getFfmpeg().getExecutable());
        command.add("-y");
        command.add("-f");
        command.add("concat");
        command.add("-safe");
        command.add("0");
        command.add("-i");
        command.add(listFile);
        command.add("-c");
        command.add("copy");
        command.add(outputPath);

        executeCommand(command, "concatenateScenes");
        return outputPath;
    }

    /**
     * Add audio narration and subtitles to video
     */
    private String addAudioAndSubtitles(String videoPath, String audioPath, 
                                        String subtitlePath, String outputPath) {
        List<String> command = new ArrayList<>();
        command.add(appProperties.getFfmpeg().getExecutable());
        command.add("-y");
        command.add("-i");
        command.add(videoPath);
        command.add("-i");
        command.add(audioPath);

        if (subtitlePath != null && Files.exists(Path.of(subtitlePath))) {
            command.add("-vf");
            command.add("subtitles=" + subtitlePath + ":force_style='FontName=Arial,FontSize=24,PrimaryColour=&H00FFFFFF,OutlineColour=&H00000000,Outline=2,Alignment=2'");
        }

        command.add("-c:v");
        command.add("libx264");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");
        command.add("-ar");
        command.add("44100");
        command.add("-shortest");
        command.add("-movflags");
        command.add("+faststart");
        command.add("-preset");
        command.add("fast");
        command.add("-crf");
        command.add("23");
        command.add("-pix_fmt");
        command.add("yuv420p");
        command.add(outputPath);

        executeCommand(command, "addAudioAndSubtitles");
        return outputPath;
    }

    /**
     * Generate subtitle file from scene narrations
     */
    public String generateSubtitles(List<String> narrations, List<Integer> durations, String outputPath) {
        log.info("Generating subtitles for {} scenes", narrations.size());

        StringBuilder srt = new StringBuilder();
        int currentTime = 0;

        for (int i = 0; i < narrations.size(); i++) {
            int startMs = currentTime * 1000;
            int endMs = (currentTime + durations.get(i)) * 1000;

            srt.append(i + 1).append("
");
            srt.append(formatSrtTime(startMs)).append(" --> ").append(formatSrtTime(endMs)).append("
");
            srt.append(narrations.get(i)).append("

");

            currentTime += durations.get(i);
        }

        try {
            Path path = Path.of(outputPath);
            Files.createDirectories(path.getParent());
            Files.writeString(path, srt.toString());
            return outputPath;
        } catch (Exception e) {
            throw new FfmpegException("Subtitle generation failed: " + e.getMessage(), "generateSubtitles", -1);
        }
    }

    /**
     * Add background music to video (mixed with narration)
     */
    public String addBackgroundMusic(String videoPath, String musicPath, String outputPath) {
        log.info("Adding background music");

        List<String> command = new ArrayList<>();
        command.add(appProperties.getFfmpeg().getExecutable());
        command.add("-y");
        command.add("-i");
        command.add(videoPath);
        command.add("-i");
        command.add(musicPath);
        command.add("-filter_complex");
        command.add("[1:a]volume=0.3,aloop=loop=-1:size=2e+09[bg];[0:a][bg]amix=inputs=2:duration=first[aout]");
        command.add("-map");
        command.add("0:v");
        command.add("-map");
        command.add("[aout]");
        command.add("-c:v");
        command.add("copy");
        command.add("-c:a");
        command.add("aac");
        command.add("-b:a");
        command.add("128k");
        command.add("-shortest");
        command.add(outputPath);

        executeCommand(command, "addBackgroundMusic");
        return outputPath;
    }

    /**
     * Create thumbnail from video frame
     */
    public String extractThumbnail(String videoPath, String outputPath, int timestampSeconds) {
        log.info("Extracting thumbnail at {}s", timestampSeconds);

        List<String> command = new ArrayList<>();
        command.add(appProperties.getFfmpeg().getExecutable());
        command.add("-y");
        command.add("-i");
        command.add(videoPath);
        command.add("-ss");
        command.add(String.valueOf(timestampSeconds));
        command.add("-vframes");
        command.add("1");
        command.add("-q:v");
        command.add("2");
        command.add(outputPath);

        executeCommand(command, "extractThumbnail");
        return outputPath;
    }

    /**
     * Get video duration in seconds
     */
    public double getVideoDuration(String videoPath) {
        try {
            List<String> command = new ArrayList<>();
            command.add(appProperties.getFfmpeg().getFfprobe());
            command.add("-v");
            command.add("error");
            command.add("-show_entries");
            command.add("format=duration");
            command.add("-of");
            command.add("default=noprint_wrappers=1:nokey=1");
            command.add(videoPath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor(10, TimeUnit.SECONDS);

            if (line != null) {
                return Double.parseDouble(line.trim());
            }
        } catch (Exception e) {
            log.error("Failed to get video duration: {}", e.getMessage());
        }
        return 0;
    }

    /**
     * Execute FFmpeg command with error handling
     */
    private void executeCommand(List<String> command, String operation) {
        log.debug("Executing FFmpeg: {}", String.join(" ", command));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.directory(new File(appProperties.getFfmpeg().getWorkingDir()));

            Process process = pb.start();

            // Read output for debugging
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("
");
                if (output.length() > 10000) break; // Limit output size
            }

            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new FfmpegException("FFmpeg timed out after 5 minutes", operation, -1);
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("FFmpeg error output: {}", output.toString());
                throw new FfmpegException("FFmpeg exited with code " + exitCode + ": " + output.toString(), 
                        operation, exitCode);
            }

            log.debug("FFmpeg completed successfully for: {}", operation);

        } catch (FfmpegException e) {
            throw e;
        } catch (Exception e) {
            throw new FfmpegException("FFmpeg execution failed: " + e.getMessage(), operation, -1);
        }
    }

    private void cleanupTempFiles(List<String> sceneVideos, String concatenatedVideo) {
        for (String file : sceneVideos) {
            try {
                Files.deleteIfExists(Path.of(file));
            } catch (Exception e) {
                log.warn("Failed to delete temp file: {}", file);
            }
        }
        try {
            Files.deleteIfExists(Path.of(concatenatedVideo));
            Files.deleteIfExists(Path.of(appProperties.getFfmpeg().getTempDir() + "/concat_list.txt"));
        } catch (Exception e) {
            log.warn("Failed to delete temp files");
        }
    }

    private String formatSrtTime(int ms) {
        int hours = ms / 3600000;
        int minutes = (ms % 3600000) / 60000;
        int seconds = (ms % 60000) / 1000;
        int millis = ms % 1000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, millis);
    }
}

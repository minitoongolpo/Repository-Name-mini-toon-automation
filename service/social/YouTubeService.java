package com.minitoon.service.social;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.minitoon.config.AppProperties;
import com.minitoon.exception.SocialUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * YouTube Service - Uploads Shorts with metadata
 * Uses YouTube Data API v3 with OAuth2 authentication
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YouTubeService {

    private final AppProperties appProperties;
    private static final String APPLICATION_NAME = "MiniToon AI Automation";
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/youtube.upload");

    /**
     * Upload video as YouTube Shorts
     */
    public String uploadShorts(String videoPath, String title, String description, 
                                String hashtags, String thumbnailPath) {
        log.info("Uploading YouTube Shorts: {}", title);

        if (!appProperties.getSocial().getYoutube().isEnabled()) {
            log.warn("YouTube upload is disabled");
            return null;
        }

        try {
            YouTube youtube = getYouTubeService();

            // Video metadata
            Video video = new Video();
            VideoSnippet snippet = new VideoSnippet();
            snippet.setTitle(title + " #Shorts " + hashtags);
            snippet.setDescription(description + "

" + hashtags);
            snippet.setTags(parseTags(hashtags));
            snippet.setCategoryId(appProperties.getSocial().getYoutube().getCategoryId());

            VideoStatus status = new VideoStatus();
            status.setPrivacyStatus(appProperties.getSocial().getYoutube().getPrivacyStatus());
            status.setSelfDeclaredMadeForKids(true); // Kids-friendly content

            video.setSnippet(snippet);
            video.setStatus(status);

            // Upload video
            File videoFile = new File(videoPath);
            FileContent mediaContent = new FileContent("video/mp4", videoFile);

            YouTube.Videos.Insert videoInsert = youtube.videos()
                    .insert("snippet,status", video, mediaContent);
            videoInsert.setNotifySubscribers(false);

            Video returnedVideo = videoInsert.execute();
            String videoId = returnedVideo.getId();

            log.info("YouTube Shorts uploaded successfully. Video ID: {}", videoId);

            // Upload thumbnail if available
            if (thumbnailPath != null && new File(thumbnailPath).exists()) {
                uploadThumbnail(youtube, videoId, thumbnailPath);
            }

            return videoId;

        } catch (Exception e) {
            throw new SocialUploadException("YouTube", videoPath, "Upload failed: " + e.getMessage(), e);
        }
    }

    /**
     * Upload thumbnail for a video
     */
    private void uploadThumbnail(YouTube youtube, String videoId, String thumbnailPath) {
        try {
            File thumbnailFile = new File(thumbnailPath);
            FileContent mediaContent = new FileContent("image/png", thumbnailFile);
            youtube.thumbnails().set(videoId, mediaContent).execute();
            log.info("Thumbnail uploaded for video: {}", videoId);
        } catch (Exception e) {
            log.error("Failed to upload thumbnail: {}", e.getMessage());
        }
    }

    /**
     * Get YouTube service with OAuth2 credentials
     */
    private YouTube getYouTubeService() throws Exception {
        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // For production/cloud deployment, use refresh token flow
        Credential credential = createCredentialWithRefreshToken(httpTransport);

        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Create credential using refresh token (for cloud deployment)
     */
    private Credential createCredentialWithRefreshToken(NetHttpTransport httpTransport) throws IOException {
        String clientId = appProperties.getSocial().getYoutube().getClientId();
        String clientSecret = appProperties.getSocial().getYoutube().getClientSecret();
        String refreshToken = appProperties.getSocial().getYoutube().getRefreshToken();

        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new SocialUploadException("YouTube", "", "No refresh token configured. " +
                    "Run OAuth2 flow locally first to obtain refresh token.");
        }

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets();
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details();
        details.setClientId(clientId);
        details.setClientSecret(clientSecret);
        clientSecrets.setInstalled(details);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setAccessType("offline")
                .build();

        Credential credential = new Credential.Builder(flow.getMethod())
                .setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setClientAuthentication(flow.getClientAuthentication())
                .setTokenServerEncodedUrl(flow.getTokenServerEncodedUrl())
                .build();

        credential.setRefreshToken(refreshToken);
        credential.refreshToken();

        return credential;
    }

    /**
     * Parse hashtags into tags list
     */
    private List<String> parseTags(String hashtags) {
        return java.util.Arrays.stream(hashtags.split("\s+"))
                .map(tag -> tag.replace("#", "").trim())
                .filter(tag -> !tag.isEmpty())
                .limit(15) // YouTube allows max 500 chars in tags
                .toList();
    }

    /**
     * Check if YouTube upload is configured
     */
    public boolean isConfigured() {
        AppProperties.SocialProperties.YoutubeProperties yt = appProperties.getSocial().getYoutube();
        return yt.isEnabled() 
                && yt.getClientId() != null && !yt.getClientId().isEmpty()
                && yt.getClientSecret() != null && !yt.getClientSecret().isEmpty()
                && yt.getRefreshToken() != null && !yt.getRefreshToken().isEmpty();
    }
}

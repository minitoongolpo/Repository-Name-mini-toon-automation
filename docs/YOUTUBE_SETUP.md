# YouTube Data API v3 Setup Guide

## Step 1: Create Google Cloud Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project (e.g., "MiniToon Automation")
3. Enable billing (required for API usage)

## Step 2: Enable YouTube Data API
1. Navigate to **APIs & Services > Library**
2. Search for "YouTube Data API v3"
3. Click **Enable**

## Step 3: Create OAuth2 Credentials
1. Go to **APIs & Services > Credentials**
2. Click **Create Credentials > OAuth client ID**
3. Select **Desktop app** as application type
4. Name it "MiniToon Desktop Client"
5. Download the `client_secret.json`

## Step 4: Get Refresh Token (One-time setup)
Run this Java code locally to get a refresh token:

```java
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.*;
import java.util.*;

public class YouTubeAuth {
    public static void main(String[] args) throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
            JacksonFactory.getDefaultInstance(),
            new FileReader("client_secret.json")
        );

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
            new NetHttpTransport(),
            JacksonFactory.getDefaultInstance(),
            clientSecrets,
            Collections.singletonList("https://www.googleapis.com/auth/youtube.upload")
        ).setAccessType("offline").build();

        // Open browser for user consent
        String url = flow.newAuthorizationUrl()
            .setRedirectUri("http://localhost:8080/Callback")
            .build();

        System.out.println("Open this URL in browser: " + url);
        System.out.println("Enter authorization code: ");

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String code = br.readLine();

        GoogleTokenResponse tokenResponse = flow.newTokenRequest(code)
            .setRedirectUri("http://localhost:8080/Callback")
            .execute();

        System.out.println("Refresh Token: " + tokenResponse.getRefreshToken());
        System.out.println("Access Token: " + tokenResponse.getAccessToken());
    }
}
```

## Step 5: Configure Environment Variables
Add to your `.env` file:
```
YOUTUBE_CLIENT_ID=your_client_id.apps.googleusercontent.com
YOUTUBE_CLIENT_SECRET=your_client_secret
YOUTUBE_REFRESH_TOKEN=your_refresh_token
YOUTUBE_CHANNEL_ID=your_channel_id
```

## Step 6: Find Your Channel ID
1. Go to [YouTube Studio](https://studio.youtube.com/)
2. Settings > Channel > Basic info
3. Copy the Channel ID

## Important Notes
- Refresh tokens don't expire unless revoked
- Store refresh token securely (environment variable)
- The app needs "YouTube Data API v3" scope: `https://www.googleapis.com/auth/youtube.upload`
- For production, use a **Desktop app** OAuth client (not Web application)

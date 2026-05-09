# Render.com Deployment Guide

## Step 1: Prepare Repository
1. Push code to GitHub
2. Ensure `render.yaml` is in root
3. Ensure `Dockerfile` is in root

## Step 2: Create Render Account
1. Sign up at [render.com](https://render.com)
2. Connect your GitHub account

## Step 3: Deploy Web Service
1. Click **New +** > **Web Service**
2. Connect your repository
3. Render will auto-detect `render.yaml`
4. Click **Create Web Service**

## Step 4: Add Environment Variables
In Render Dashboard > Your Service > Environment:
```
GEMINI_API_KEY=xxx
LEONARDO_API_KEY=xxx
ELEVENLABS_API_KEY=xxx
YOUTUBE_REFRESH_TOKEN=xxx
INSTAGRAM_ACCESS_TOKEN=xxx
FACEBOOK_ACCESS_TOKEN=xxx
```

## Step 5: Create PostgreSQL Database
1. Render Dashboard > **New +** > **PostgreSQL**
2. Name: `minitoon-db`
3. Plan: Standard (or Free for testing)
4. Render will auto-link via `render.yaml`

## Step 6: Verify Deployment
```bash
curl https://your-service-name.onrender.com/api/v1/status/health
```

## Cron Jobs on Render
Render cron jobs are configured in `render.yaml`:
- `minitoon-daily-gen`: Triggers video generation at 6 AM
- `minitoon-daily-upload`: Triggers upload at 8 AM

## Important Notes
- Free tier spins down after 15 min inactivity
- Use Standard tier for 24/7 operation
- FFmpeg is pre-installed in Docker image
- File system is ephemeral — use external storage for persistence

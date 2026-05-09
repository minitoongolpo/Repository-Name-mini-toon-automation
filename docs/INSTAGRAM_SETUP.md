# Instagram Graph API Setup Guide

## Prerequisites
- Business Instagram Account (not personal)
- Facebook Business Page connected to Instagram
- Facebook Developer Account

## Step 1: Create Facebook App
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create new app > Business type
3. Add "Instagram Graph API" product

## Step 2: Connect Instagram Account
1. Go to App Dashboard > Instagram Graph API > Basic Display
2. Add Instagram Tester
3. Add your Instagram business account
4. Accept invitation in Instagram app

## Step 3: Get Access Token
1. Go to **Graph API Explorer** (https://developers.facebook.com/tools/explorer/)
2. Select your app
3. Generate User Access Token with permissions:
   - `instagram_basic`
   - `instagram_content_publish`
   - `pages_read_engagement`

## Step 4: Get Instagram Account ID
Use Graph API Explorer:
```
GET /me/accounts
```
Find your page, then:
```
GET /{page-id}?fields=instagram_business_account
```
The `instagram_business_account.id` is your **Account ID**.

## Step 5: Convert to Long-lived Token
```bash
curl -X GET "https://graph.facebook.com/v22.0/oauth/access_token?grant_type=fb_exchange_token&client_id=YOUR_APP_ID&client_secret=YOUR_APP_SECRET&fb_exchange_token=SHORT_LIVED_TOKEN"
```

## Step 6: Configure Environment
```
INSTAGRAM_ACCESS_TOKEN=your_long_lived_token
INSTAGRAM_ACCOUNT_ID=your_account_id
```

## Important Notes
- Instagram requires **publicly accessible video URLs** for upload
- For cloud deployment, upload videos to a public CDN/S3 first
- Token expires after ~60 days; implement refresh logic
- Reels must be 9:16 aspect ratio, under 90 seconds

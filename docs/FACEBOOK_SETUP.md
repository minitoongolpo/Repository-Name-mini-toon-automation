# Facebook Graph API Setup Guide

## Step 1: Create Facebook App
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create App > Business type
3. Add "Graph API" product

## Step 2: Get Page Access Token
1. Go to [Graph API Explorer](https://developers.facebook.com/tools/explorer/)
2. Select your app
3. Get User Access Token with `pages_manage_posts` permission
4. Exchange for Page Access Token:
```
GET /me/accounts?access_token=USER_TOKEN
```
Use the `access_token` from your page object.

## Step 3: Get Page ID
Same API call returns `id` field for your page.

## Step 4: Configure Environment
```
FACEBOOK_ACCESS_TOKEN=your_page_access_token
FACEBOOK_PAGE_ID=your_page_id
```

## Important Notes
- Page Access Token is permanent (doesn't expire)
- Video must be publicly accessible URL
- Supported formats: MP4, MOV
- Max file size: 10GB

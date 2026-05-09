# 🎬 MiniToon AI Automation

**Production-ready full-stack Java Spring Boot application for automatic Bengali YouTube Shorts & Instagram Reels generation and upload.**

Runs fully automatically in the cloud (24/7) — generates AI-powered kids-friendly Bengali stories, renders them into vertical videos, and uploads to YouTube, Instagram, and Facebook daily.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🤖 **AI Story Generation** | Gemini API creates unique Bengali stories daily |
| 🎨 **AI Image Generation** | Leonardo AI generates cartoon/cinematic scenes |
| 🗣️ **AI Voice Narration** | ElevenLabs produces Bengali kids-friendly narration |
| 🎬 **FFmpeg Video Rendering** | Combines images + audio + subtitles + transitions |
| 📤 **Auto YouTube Upload** | YouTube Data API v3 with OAuth2 |
| 📸 **Auto Instagram Reels** | Instagram Graph API container-based upload |
| 📘 **Auto Facebook Videos** | Facebook Graph API page video upload |
| ⏰ **Daily Scheduler** | Cron-based automatic generation & upload |
| 🐳 **Docker Ready** | Single-command deployment |
| ☁️ **Render/VPS Ready** | Production deployment configs included |

---

## 🏗️ Architecture

```
Daily Scheduler (6 AM)
    ↓
Gemini → Bengali Story
    ↓
Gemini → Scene Breakdown + Prompts
    ↓
Leonardo AI → Cartoon Scene Images
    ↓
ElevenLabs → Bengali Narration Audio
    ↓
FFmpeg → Vertical Video (720x1280)
    ↓
Gemini → Title + Description + Hashtags
    ↓
Leonardo AI → Thumbnail
    ↓
YouTube Shorts Upload (8 AM)
    ↓
Instagram Reels Upload
    ↓
Facebook Page Upload
    ↓
PostgreSQL Logging
```

---

## 🛠️ Tech Stack

- **Java 21** + **Spring Boot 3.2**
- **Maven** build system
- **PostgreSQL** database
- **FFmpeg** video engine
- **Docker** + **Docker Compose**
- **WebFlux** reactive HTTP client
- **Spring Scheduler** cron jobs
- **Spring Data JPA** persistence

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker & Docker Compose
- FFmpeg installed locally (for development)

### 1. Clone & Build
```bash
git clone https://github.com/yourusername/mini-toon-automation.git
cd mini-toon-automation
mvn clean package -DskipTests
```

### 2. Configure Environment
```bash
cp .env.example .env
# Edit .env with your API keys
```

### 3. Run with Docker Compose
```bash
docker-compose up -d
```

### 4. Verify
```bash
curl http://localhost:8080/api/v1/status/health
```

---

## 📁 Project Structure

```
mini-toon-automation/
├── src/main/java/com/minitoon/
│   ├── MiniToonApplication.java
│   ├── config/              # Configuration classes
│   ├── controller/          # REST API controllers
│   ├── service/             # Business logic
│   │   ├── ai/              # AI services (Gemini, Leonardo, ElevenLabs)
│   │   ├── ffmpeg/          # FFmpeg video rendering
│   │   ├── social/          # Social media upload services
│   │   └── scheduler/       # Cron job schedulers
│   ├── model/               # JPA entities
│   ├── dto/                 # Data transfer objects
│   ├── repository/          # Spring Data repositories
│   ├── utils/               # Utility classes
│   └── exception/           # Custom exceptions
├── src/main/resources/
│   ├── application.yml        # Main configuration
│   └── scripts/             # FFmpeg helper scripts
├── Dockerfile               # Docker build
├── docker-compose.yml       # Local orchestration
├── render.yaml              # Render.com deployment
├── pom.xml                  # Maven dependencies
└── docs/                    # Setup guides
```

---

## 🔌 API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/videos/generate` | Manually generate a video |
| `GET` | `/api/v1/videos` | List all videos |
| `GET` | `/api/v1/videos/{id}` | Get video details |
| `POST` | `/api/v1/videos/{id}/upload` | Upload to all platforms |
| `POST` | `/api/v1/videos/{id}/retry` | Retry failed video |
| `GET` | `/api/v1/status/health` | Health check |
| `GET` | `/api/v1/status/services` | Service configuration status |

---

## ☁️ Deployment

### Option A: Render.com (Recommended)
1. Fork this repo to GitHub
2. Connect Render to your repo
3. Add environment variables in Render Dashboard
4. Deploy — runs 24/7 automatically

See [docs/RENDER_DEPLOYMENT.md](docs/RENDER_DEPLOYMENT.md)

### Option B: VPS (DigitalOcean, AWS, etc.)
1. Provision Ubuntu 22.04+ server
2. Install Docker & Docker Compose
3. Clone repo and run `docker-compose up -d`
4. Systemd service auto-starts on boot

See [docs/VPS_DEPLOYMENT.md](docs/VPS_DEPLOYMENT.md)

---

## 🔑 API Setup Guides

- [YouTube API Setup](docs/YOUTUBE_SETUP.md)
- [Instagram API Setup](docs/INSTAGRAM_SETUP.md)
- [Facebook API Setup](docs/FACEBOOK_SETUP.md)

---

## ⚙️ Configuration

All settings in `application.yml` or environment variables:

```yaml
app:
  ai:
    gemini:
      api-key: ${GEMINI_API_KEY}
      model: gemini-2.0-flash
    leonardo:
      api-key: ${LEONARDO_API_KEY}
    elevenlabs:
      api-key: ${ELEVENLABS_API_KEY}
      voice-id: ${ELEVENLABS_VOICE_ID}
  video:
    width: 720
    height: 1280
    duration-seconds: 35
  scheduler:
    daily-generation-time: "0 0 6 * * *"  # 6 AM
    daily-upload-time: "0 0 8 * * *"      # 8 AM
```

---

## 📝 License

MIT License - see LICENSE file

---

## 🤝 Contributing

Pull requests welcome! Please follow the existing code style and add tests.

---

**Made with ❤️ for Bengali content creators**

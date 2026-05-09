#!/bin/bash
# MiniToon Deployment Script

set -e

echo "🎬 MiniToon AI Automation - Deployment Script"
echo "=============================================="

# Check prerequisites
echo "Checking prerequisites..."
command -v docker >/dev/null 2>&1 || { echo "Docker required but not installed. Aborting." >&2; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "Docker Compose required but not installed. Aborting." >&2; exit 1; }

# Check .env file
if [ ! -f .env ]; then
    echo "⚠️  .env file not found. Copying from example..."
    cp .env.example .env
    echo "✏️  Please edit .env with your API keys before continuing"
    exit 1
fi

# Build and deploy
echo "Building Docker images..."
docker-compose build --no-cache

echo "Starting services..."
docker-compose up -d

echo "Waiting for services to start..."
sleep 10

echo "Checking health..."
if curl -s http://localhost:8080/api/v1/status/health | grep -q '"success":true'; then
    echo "✅ MiniToon is running successfully!"
    echo "Health check: http://localhost:8080/api/v1/status/health"
    echo "API docs: http://localhost:8080/api/v1/status/services"
else
    echo "❌ Health check failed. Check logs:"
    echo "docker-compose logs -f app"
fi

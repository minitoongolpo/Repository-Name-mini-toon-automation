# VPS Deployment Guide (Ubuntu 22.04)

## Step 1: Server Setup
```bash
# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
sudo apt install docker.io docker-compose -y
sudo systemctl enable docker
sudo usermod -aG docker $USER

# Install FFmpeg
sudo apt install ffmpeg -y

# Create app user
sudo useradd -m -s /bin/bash minitoon
```

## Step 2: Deploy Application
```bash
sudo su - minitoon
git clone https://github.com/yourusername/mini-toon-automation.git
cd mini-toon-automation

# Create environment file
cp .env.example .env
nano .env  # Add your API keys

# Build and run
docker-compose up -d --build
```

## Step 3: Setup Systemd Service
```bash
sudo cp scripts/minitoon.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable minitoon
sudo systemctl start minitoon
```

## Step 4: Setup Nginx (Optional)
```bash
sudo apt install nginx -y
sudo nano /etc/nginx/sites-available/minitoon
```

Add:
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/minitoon /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

## Step 5: SSL with Certbot
```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
```

## Step 6: Monitoring
```bash
# View logs
sudo journalctl -u minitoon -f

# Check status
sudo systemctl status minitoon

# Restart service
sudo systemctl restart minitoon
```

## Important Notes
- Ensure firewall allows ports 80, 443, 8080
- Use `ufw` for simple firewall management
- Set up log rotation for `/app/logs`
- Consider using `fail2ban` for security

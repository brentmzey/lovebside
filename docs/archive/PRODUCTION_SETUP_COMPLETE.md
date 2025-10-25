# 🚀 COMPLETE PRODUCTION SETUP GUIDE

## 📋 Table of Contents

1. [Production Database Setup](#production-database-setup)
2. [Server Infrastructure Setup](#server-infrastructure-setup)
3. [GitHub Actions CI/CD](#github-actions-cicd)
4. [Distribution & Deployment](#distribution--deployment)
5. [Monitoring & Maintenance](#monitoring--maintenance)

---

## 1️⃣ PRODUCTION DATABASE SETUP

### Option A: Self-Hosted PocketBase (Recommended - Simple)

**Step 1: Install PocketBase on Production Server**

```bash
# SSH into your production server
ssh user@your-production-server.com

# Download PocketBase (latest version)
cd /opt
sudo mkdir bside-prod
cd bside-prod
sudo wget https://github.com/pocketbase/pocketbase/releases/download/v0.22.0/pocketbase_0.22.0_linux_amd64.zip
sudo unzip pocketbase_0.22.0_linux_amd64.zip
sudo chmod +x pocketbase

# Create data directory
sudo mkdir -p pb_data

# Test run
./pocketbase serve --http=127.0.0.1:8090
```

**Step 2: Create Systemd Service for PocketBase**

```bash
sudo nano /etc/systemd/system/pocketbase.service
```

Add this content:

```ini
[Unit]
Description=PocketBase Database Service
After=network.target

[Service]
Type=simple
User=www-data
Group=www-data
WorkingDirectory=/opt/bside-prod
ExecStart=/opt/bside-prod/pocketbase serve --http=127.0.0.1:8090
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start
sudo systemctl daemon-reload
sudo systemctl enable pocketbase
sudo systemctl start pocketbase
sudo systemctl status pocketbase
```

**Step 3: Initialize Production Schema**

```bash
# Access PocketBase admin UI
# Visit: http://your-server:8090/_/
# Create admin account
# Import schema from: /Users/brentzey/bside/pocketbase/pb_schema.json
```

### Option B: PostgreSQL (Enterprise-Grade)

**Step 1: Install PostgreSQL**

```bash
# On Ubuntu/Debian
sudo apt update
sudo apt install postgresql postgresql-contrib

# On CentOS/RHEL
sudo yum install postgresql-server postgresql-contrib
sudo postgresql-setup initdb
```

**Step 2: Create Production Database**

```bash
sudo -u postgres psql

CREATE DATABASE bside_production;
CREATE USER bside_app WITH ENCRYPTED PASSWORD 'your-secure-password-here';
GRANT ALL PRIVILEGES ON DATABASE bside_production TO bside_app;
\q
```

**Step 3: Configure Remote Access** (if needed)

```bash
# Edit postgresql.conf
sudo nano /etc/postgresql/14/main/postgresql.conf
# Set: listen_addresses = '*'

# Edit pg_hba.conf
sudo nano /etc/postgresql/14/main/pg_hba.conf
# Add: host bside_production bside_app 0.0.0.0/0 md5

sudo systemctl restart postgresql
```

---

## 2️⃣ SERVER INFRASTRUCTURE SETUP

### Production Server Requirements

**Minimum Specs:**
- **CPU:** 2 cores
- **RAM:** 2GB (4GB recommended)
- **Disk:** 20GB SSD
- **OS:** Ubuntu 22.04 LTS (recommended)
- **Network:** Static IP or domain name

**Recommended Providers:**
- DigitalOcean ($12/month)
- AWS Lightsail ($10/month)
- Linode ($12/month)
- Hetzner ($5/month)

### Step 1: Initial Server Setup

```bash
# SSH into server
ssh root@your-server.com

# Update system
apt update && apt upgrade -y

# Create deploy user
adduser deploy
usermod -aG sudo deploy

# Setup SSH key authentication
mkdir -p /home/deploy/.ssh
cp ~/.ssh/authorized_keys /home/deploy/.ssh/
chown -R deploy:deploy /home/deploy/.ssh
chmod 700 /home/deploy/.ssh
chmod 600 /home/deploy/.ssh/authorized_keys

# Switch to deploy user
su - deploy
```

### Step 2: Install Required Software

```bash
# Install Java (for Ktor server)
sudo apt install openjdk-17-jre-headless -y

# Install Nginx (reverse proxy)
sudo apt install nginx -y

# Install Certbot (SSL certificates)
sudo apt install certbot python3-certbot-nginx -y

# Install Docker (optional, for containerized deployment)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker deploy

# Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### Step 3: Configure Nginx Reverse Proxy

```bash
sudo nano /etc/nginx/sites-available/bside
```

Add this configuration:

```nginx
server {
    listen 80;
    server_name api.your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /_/ {
        proxy_pass http://127.0.0.1:8090;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

```bash
# Enable site
sudo ln -s /etc/nginx/sites-available/bside /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### Step 4: Setup SSL Certificate

```bash
sudo certbot --nginx -d api.your-domain.com
# Follow prompts, select "2" for redirect HTTP to HTTPS
```

### Step 5: Create Systemd Service for Backend

```bash
sudo nano /etc/systemd/system/bside-backend.service
```

Add:

```ini
[Unit]
Description=B-Side Backend Server
After=network.target pocketbase.service

[Service]
Type=simple
User=deploy
Group=deploy
WorkingDirectory=/opt/bside-prod
Environment="DATABASE_URL=http://127.0.0.1:8090"
Environment="PORT=8080"
Environment="ENVIRONMENT=production"
ExecStart=/usr/bin/java -jar /opt/bside-prod/server-1.0.0-all.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl daemon-reload
sudo systemctl enable bside-backend
```

---

## 3️⃣ GITHUB ACTIONS CI/CD

### Step 1: Configure GitHub Secrets

Go to: `https://github.com/brentmzey/lovebside/settings/secrets/actions`

**Required Secrets:**

1. **SERVER_HOST**
   ```
   your-production-server.com
   ```

2. **SERVER_USER**
   ```
   deploy
   ```

3. **SERVER_SSH_KEY**
   ```
   # Copy your private key
   cat ~/.ssh/id_rsa
   # Paste entire content into GitHub Secret
   ```

4. **POCKETBASE_URL** (Production)
   ```
   https://api.your-domain.com
   ```

5. **ANDROID_KEYSTORE_BASE64** (For signed APKs)
   ```bash
   # Generate keystore
   keytool -genkey -v -keystore bside-release.keystore -alias bside -keyalg RSA -keysize 2048 -validity 10000
   
   # Encode to base64
   base64 -i bside-release.keystore | pbcopy
   # Paste into GitHub Secret
   ```

6. **KEYSTORE_PASSWORD**
   ```
   your-keystore-password
   ```

7. **KEY_ALIAS**
   ```
   bside
   ```

8. **KEY_PASSWORD**
   ```
   your-key-password
   ```

**Optional Secrets:**

- **DOCKER_USERNAME** - Docker Hub username
- **DOCKER_PASSWORD** - Docker Hub token
- **SLACK_WEBHOOK** - Slack notifications
- **SENTRY_DSN** - Error tracking

### Step 2: Verify Workflows

Your GitHub Actions workflows are already configured in `.github/workflows/`:

1. **ci-cd.yml** - Main pipeline (builds all platforms)
2. **release.yml** - Release builds with signing
3. **nightly.yml** - Nightly builds
4. **pr-check.yml** - PR validation

### Step 3: Enable GitHub Actions

1. Go to: `https://github.com/brentmzey/lovebside/actions`
2. Click **"I understand my workflows, go ahead and enable them"**
3. Done! Workflows will trigger on push

### Step 4: First Deployment

```bash
# From your local machine
cd /Users/brentzey/bside

# Push to trigger CI/CD
git add .
git commit -m "Production deployment: Enable CI/CD"
git push origin development

# Watch builds at:
# https://github.com/brentmzey/lovebside/actions
```

**What Happens Automatically:**

1. ✅ Code checkout
2. ✅ Build backend JAR (27MB)
3. ✅ Build Android APK (20MB)
4. ✅ Build desktop installers (DMG, MSI, DEB)
5. ✅ Build web distribution
6. ✅ Run all tests
7. ✅ Security scanning
8. ✅ Upload artifacts (30-day retention)
9. ✅ Deploy to production server (if on `main` branch)

---

## 4️⃣ DISTRIBUTION & DEPLOYMENT

### Backend Deployment (Automatic)

**When you push to `main` branch:**

```bash
git checkout main
git merge development
git push origin main
```

**GitHub Actions will:**
1. SSH into your production server
2. Copy new JAR to `/opt/bside-prod/`
3. Restart backend service
4. Verify health check
5. Notify on Slack (if configured)

**Manual Deployment (if needed):**

```bash
# Download artifact from GitHub Actions
# Then:
scp server-1.0.0-all.jar deploy@your-server:/opt/bside-prod/
ssh deploy@your-server 'sudo systemctl restart bside-backend'
```

### Android Distribution

**Option 1: Google Play Store (Recommended)**

1. Download signed APK from GitHub Actions artifacts
2. Go to: `https://play.google.com/console`
3. Create app listing
4. Upload APK to internal testing
5. Add testers
6. Promote to production when ready

**Option 2: Direct APK Distribution**

```bash
# Host on your website
scp composeApp-release.apk deploy@your-server:/var/www/html/downloads/
# Users download from: https://your-domain.com/downloads/composeApp-release.apk
```

### iOS Distribution

**Step 1: Build in Xcode**

```bash
# On Mac
cd /Users/brentzey/bside
open iosApp/iosApp.xcodeproj

# In Xcode:
# 1. Select "Any iOS Device" as target
# 2. Product → Archive
# 3. Wait for archive to complete
```

**Step 2: Distribute via App Store Connect**

1. Window → Organizer
2. Select your archive
3. Click "Distribute App"
4. Choose "App Store Connect"
5. Follow prompts
6. Submit for review

**Step 3: TestFlight (Beta Testing)**

1. Same steps as above
2. Choose "TestFlight" distribution
3. Add internal/external testers
4. Share invite link

### Desktop Distribution

**macOS (DMG)**

```bash
# Download from GitHub Actions
# Host on website or use GitHub Releases

# Users install:
# 1. Download DMG
# 2. Double-click to mount
# 3. Drag app to Applications folder
```

**Windows (MSI)**

```bash
# Download from GitHub Actions
# Host on website

# Users install:
# 1. Download MSI
# 2. Double-click
# 3. Follow installation wizard
```

**Linux (DEB)**

```bash
# Download from GitHub Actions
# Host in APT repository or direct download

# Users install:
sudo dpkg -i bside-app.deb
# Or double-click in file manager
```

### Web Deployment

**Option 1: Netlify (Easiest)**

```bash
# Install Netlify CLI
npm install -g netlify-cli

# Login
netlify login

# Deploy
cd /Users/brentzey/bside
./gradlew :composeApp:jsBrowserDistribution
cd composeApp/build/distributions
netlify deploy --prod --dir=.
```

**Option 2: Vercel**

```bash
npm install -g vercel
vercel --prod composeApp/build/distributions
```

**Option 3: Self-Hosted (Nginx)**

```bash
# Build web distribution
./gradlew :composeApp:jsBrowserDistribution

# Copy to server
scp -r composeApp/build/distributions/* deploy@your-server:/var/www/html/app/

# Configure Nginx
sudo nano /etc/nginx/sites-available/bside-web
```

Add:

```nginx
server {
    listen 80;
    server_name app.your-domain.com;

    root /var/www/html/app;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/bside-web /etc/nginx/sites-enabled/
sudo systemctl reload nginx
sudo certbot --nginx -d app.your-domain.com
```

---

## 5️⃣ MONITORING & MAINTENANCE

### Health Checks

**Backend Health Endpoint:**
```bash
curl https://api.your-domain.com/health
# Should return: {"status":"healthy","version":"1.0.0"}
```

**Automated Monitoring:**

```bash
# Install monitoring service on server
sudo apt install monit

sudo nano /etc/monit/conf.d/bside
```

Add:

```
check process bside-backend with pidfile /var/run/bside-backend.pid
    start program = "/bin/systemctl start bside-backend"
    stop program = "/bin/systemctl stop bside-backend"
    if failed host 127.0.0.1 port 8080 protocol http
        request "/health"
        with timeout 10 seconds
        for 3 cycles
    then restart
```

### Log Management

**View Logs:**

```bash
# Backend logs
sudo journalctl -u bside-backend -f

# PocketBase logs
sudo journalctl -u pocketbase -f

# Nginx logs
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

**Log Rotation:**

```bash
sudo nano /etc/logrotate.d/bside
```

Add:

```
/var/log/bside/*.log {
    daily
    rotate 30
    compress
    delaycompress
    notifempty
    create 0640 deploy deploy
    sharedscripts
}
```

### Backup Strategy

**Database Backups (PocketBase):**

```bash
# Create backup script
sudo nano /opt/bside-prod/backup-db.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/opt/bside-prod/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

mkdir -p $BACKUP_DIR

# Backup PocketBase data
tar -czf $BACKUP_DIR/pocketbase_$TIMESTAMP.tar.gz /opt/bside-prod/pb_data

# Keep only last 30 days
find $BACKUP_DIR -name "pocketbase_*.tar.gz" -mtime +30 -delete

echo "Backup completed: $BACKUP_DIR/pocketbase_$TIMESTAMP.tar.gz"
```

```bash
chmod +x /opt/bside-prod/backup-db.sh

# Add to crontab (daily at 2 AM)
crontab -e
0 2 * * * /opt/bside-prod/backup-db.sh
```

**Code Backups:**

Your code is already backed up on GitHub! 🎉

### Security Updates

```bash
# Enable automatic security updates
sudo apt install unattended-upgrades
sudo dpkg-reconfigure --priority=low unattended-upgrades
```

### Performance Monitoring

**Option 1: Prometheus + Grafana**

```bash
# Install Prometheus
docker run -d -p 9090:9090 prom/prometheus

# Install Grafana
docker run -d -p 3000:3000 grafana/grafana
```

**Option 2: Simple Monitoring**

```bash
# Install htop
sudo apt install htop

# Check system resources
htop

# Check disk usage
df -h

# Check memory
free -h
```

---

## 🎯 COMPLETE DEPLOYMENT CHECKLIST

### Pre-Launch Checklist

- [ ] Production server provisioned and configured
- [ ] Database (PocketBase or PostgreSQL) running
- [ ] Domain name configured and DNS pointing to server
- [ ] SSL certificate installed (HTTPS)
- [ ] GitHub Actions secrets configured
- [ ] Backend service running and accessible
- [ ] Health check returning 200 OK
- [ ] Monitoring and alerts configured
- [ ] Backup strategy implemented
- [ ] Security updates enabled

### Launch Checklist

- [ ] Push code to GitHub
- [ ] Verify CI/CD builds complete successfully
- [ ] Download all artifacts from GitHub Actions
- [ ] Deploy backend to production server
- [ ] Verify backend health check
- [ ] Test API endpoints
- [ ] Upload Android APK to Play Store (internal testing)
- [ ] Build and distribute iOS app via TestFlight
- [ ] Host desktop installers (DMG, MSI, DEB)
- [ ] Deploy web app to hosting platform
- [ ] Test all platforms end-to-end
- [ ] Monitor logs for errors
- [ ] Set up analytics (optional)
- [ ] Set up crash reporting (Sentry, Firebase Crashlytics)

### Post-Launch Checklist

- [ ] Monitor server performance
- [ ] Check error logs daily (first week)
- [ ] Respond to user feedback
- [ ] Plan first update/hotfix
- [ ] Scale infrastructure if needed
- [ ] Set up automated alerts
- [ ] Document any issues encountered
- [ ] Create runbook for common operations

---

## 🚀 QUICK START COMMANDS

### Local Development
```bash
./build-and-run.sh
```

### Deploy to Production
```bash
git push origin main
```

### Check Production Status
```bash
curl https://api.your-domain.com/health
```

### View Logs
```bash
ssh deploy@your-server 'sudo journalctl -u bside-backend -f'
```

### Restart Services
```bash
ssh deploy@your-server 'sudo systemctl restart bside-backend pocketbase'
```

---

## 📞 SUPPORT & RESOURCES

### Documentation
- **DEPLOYMENT_STEPS.md** - Step-by-step deployment guide
- **CI_CD_COMPLETE.md** - CI/CD pipeline details
- **SHARED_TYPES_GUIDE.md** - Architecture documentation
- **PUSH_TO_GITHUB.md** - GitHub deployment guide

### Useful Commands
```bash
# Check service status
sudo systemctl status bside-backend pocketbase

# Restart all services
sudo systemctl restart bside-backend pocketbase nginx

# View all logs
sudo journalctl -xe

# Check disk space
df -h

# Check memory usage
free -h

# Check active connections
sudo netstat -tulpn | grep LISTEN
```

### Troubleshooting

**Backend won't start:**
```bash
sudo journalctl -u bside-backend -n 100
# Check for Java errors or missing dependencies
```

**Database connection failed:**
```bash
sudo systemctl status pocketbase
curl http://127.0.0.1:8090/api/health
```

**SSL certificate issues:**
```bash
sudo certbot certificates
sudo certbot renew --dry-run
```

---

## 🎉 YOU'RE READY FOR PRODUCTION!

Follow this guide step-by-step and you'll have a fully production-ready deployment with:

✅ Secure, scalable infrastructure  
✅ Automated CI/CD with GitHub Actions  
✅ Multi-platform distribution  
✅ Production database  
✅ SSL/HTTPS everywhere  
✅ Monitoring and backups  
✅ Health checks and auto-restart  

**Start here:** Push your code to GitHub and watch the magic happen! 🚀

```bash
cd /Users/brentzey/bside
git add .
git commit -m "Production deployment"
git push origin development
```

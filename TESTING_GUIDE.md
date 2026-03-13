# 🎯 B-Side Stack Testing Guide

Complete guide for testing and running B-Side in different environments.

## Quick Start

```bash
# Run the interactive walkthrough
./test-walkthrough.sh

# Or test manually
just backend          # Start dev stack
curl http://localhost:8092/api/health  # Test
just desktop          # Run app
```

## 📊 Current Status Check

```bash
# View running services
docker ps

# Test all endpoints
./test-stack.sh

# Check health
curl http://localhost:8092/api/health
curl http://localhost:8081/health
```

## 🔧 Stack Configurations

### 1. Basic Development (`docker-compose.yml`)

**What it includes:**
- PocketBase (database)
- Ktor Server (API)
- Redis (cache)
- Nginx (proxy)

**When to use:**
- Daily development
- Fast iteration
- Minimal resource usage

**Start:**
```bash
just backend
# OR
docker-compose up -d
```

**Test:**
```bash
curl http://localhost:8092/api/health
curl http://localhost:8081/health
```

**Access:**
- PocketBase: http://localhost:8092
- Admin UI: http://localhost:8092/_/
- Ktor API: http://localhost:8081

---

### 2. Enhanced Development (`docker-compose.enhanced-lite.yml`)

**What it adds:**
- Grafana (monitoring dashboards)
- Prometheus (metrics collection)
- Loki (log aggregation)

**When to use:**
- Debugging performance
- Monitoring requests
- Viewing logs centrally

**Start:**
```bash
docker-compose -f docker-compose.enhanced-lite.yml up -d
```

**Test:**
```bash
# Core services
curl http://localhost:8092/api/health
curl http://localhost:8081/health

# Monitoring
curl http://localhost:3000/api/health  # Grafana
curl http://localhost:9090/-/healthy   # Prometheus
```

**Access:**
- All basic services
- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- Metrics: http://localhost:8081/metrics

---

### 3. Full Stack (`docker-compose.full.yml`)

**What it adds:**
- Redis UI (visual Redis browser)
- Node Exporter (system metrics)
- cAdvisor (container metrics)
- GoAccess (web analytics)

**When to use:**
- Complete local testing
- Performance analysis
- Resource monitoring
- Before staging deployment

**Start:**
```bash
docker-compose -f docker-compose.full.yml up -d
```

**Test:**
```bash
# All previous tests, plus:
curl http://localhost:8083    # Redis UI
curl http://localhost:9100    # Node Exporter
curl http://localhost:8080    # cAdvisor
```

**Access:**
- All enhanced services
- Redis UI: http://localhost:8083
- cAdvisor: http://localhost:8080
- Node Exporter: http://localhost:9100/metrics

---

### 4. Production-Like (`docker-compose.production.yml`)

**What it adds:**
- Resource limits (CPU/memory)
- Restart policies
- Health checks
- Security configurations
- Environment-based config

**When to use:**
- Pre-production testing
- Staging environment
- Performance testing with limits
- Security validation

**Setup:**
```bash
# Create production env file
cp .env.example .env.production

# Edit with production values
nano .env.production

# Start
docker-compose -f docker-compose.production.yml --env-file .env.production up -d
```

**Test:**
```bash
# Same endpoints, but validate:
# - Resource limits are enforced
# - Restart policies work
# - Health checks pass
docker ps  # Check health status
docker stats  # Monitor resources
```

**Production Checklist:**
- [ ] Environment variables configured
- [ ] Resource limits appropriate
- [ ] Health checks passing
- [ ] Restart policies tested
- [ ] Security headers present
- [ ] HTTPS configured (for real prod)

---

### 5. Enterprise Stack (`docker-compose.enterprise.yml`)

**What it includes:**
- Everything from full stack
- Advanced monitoring
- Distributed tracing
- Complete observability
- All management tools

**When to use:**
- Enterprise deployments
- QA environments
- Full integration testing
- Demo environments

**Start:**
```bash
docker-compose -f docker-compose.enterprise.yml up -d
```

**Note:** This starts ~15+ containers. Allow 2-3 minutes for full startup.

---

## 🧪 Testing Workflows

### Daily Development Workflow

```bash
# 1. Start backend
just backend

# 2. Verify it's running
curl http://localhost:8092/api/health

# 3. Run your app
just desktop  # or web, android, ios

# 4. Make changes, test, iterate

# 5. Stop when done
just stop
```

### Pre-Deployment Testing

```bash
# 1. Stop current stack
just stop

# 2. Start production-like stack
docker-compose -f docker-compose.production.yml up -d

# 3. Wait for services
sleep 30

# 4. Run integration tests
./test-stack.sh

# 5. Test with real apps
just desktop

# 6. Monitor performance
open http://localhost:3000  # Grafana

# 7. Check resource usage
docker stats

# 8. Verify health checks
docker ps  # All should show (healthy)
```

### Full Integration Testing

```bash
# 1. Start full stack
docker-compose -f docker-compose.full.yml up -d

# 2. Run all tests
./test-stack.sh
./scripts/test-full-stack.sh
./scripts/test-realtime-messaging.sh

# 3. Manual testing checklist:
# - [ ] User registration
# - [ ] User login
# - [ ] Send message
# - [ ] Receive message (real-time)
# - [ ] Typing indicators
# - [ ] Read receipts
# - [ ] Reactions
# - [ ] File uploads
# - [ ] Profile updates

# 4. Check monitoring
open http://localhost:3000  # Grafana dashboards
open http://localhost:9090  # Prometheus metrics
open http://localhost:8083  # Redis data

# 5. Review logs
docker-compose logs -f --tail=100
```

---

## 🚀 Running Client Applications

### Desktop

```bash
# Quick run
just desktop

# With hot reload (experimental)
just desktop-hot

# Direct Gradle
./gradlew :composeApp:jvmRun

# Build distributable
./gradlew :composeApp:packageDmg  # macOS
./gradlew :composeApp:packageMsi  # Windows
./gradlew :composeApp:packageDeb  # Linux
```

### Web

```bash
# Development server
just web

# Direct Gradle
./gradlew :composeApp:jsBrowserDevelopmentRun

# Production build
./gradlew :composeApp:jsBrowserProductionWebpack
```

### Android

```bash
# Open Android Studio
just android-studio

# Install debug APK
just android

# Build release
./gradlew :composeApp:assembleRelease
```

### iOS

```bash
# Open Xcode
just ios

# Or directly
open iosApp/iosApp.xcodeproj
```

---

## 🔍 Monitoring & Debugging

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker logs -f bside-pocketbase
docker logs -f bside-server

# Tail last 100 lines
docker-compose logs --tail=100
```

### Check Resource Usage

```bash
# Real-time stats
docker stats

# Container details
docker inspect bside-pocketbase
```

### Access Databases

```bash
# Redis
docker exec -it bside-redis redis-cli

# PocketBase (SQLite)
docker exec -it bside-pocketbase sqlite3 /pb_data/data.db
```

### Test Endpoints

```bash
# Health checks
curl http://localhost:8092/api/health
curl http://localhost:8081/health

# Metrics
curl http://localhost:8081/metrics

# PocketBase API
curl http://localhost:8092/api/collections
```

---

## 🛠️ Troubleshooting

### Services Won't Start

```bash
# Stop everything
just stop

# Clean up
docker-compose down -v

# Rebuild
docker-compose build --no-cache

# Start fresh
just backend
```

### Port Conflicts

```bash
# Find what's using a port
lsof -i :8092
lsof -i :8081

# Kill process
kill $(lsof -t -i:8092)

# Or change ports in docker-compose.yml
```

### Build Errors

```bash
# Clean Gradle cache
./gradlew clean

# Rebuild server
./gradlew :server:shadowJar

# Full rebuild
./gradlew clean build
```

### Database Issues

```bash
# Backup database
docker exec bside-pocketbase tar czf /tmp/backup.tar.gz /pb_data

# Reset database (⚠️ destroys data)
docker-compose down -v
just backend

# Access admin UI
open http://localhost:8092/_/
```

---

## 📈 Performance Testing

### Load Testing

```bash
# Install k6
brew install k6  # macOS

# Run load test
k6 run scripts/load-test.js

# Or use Apache Bench
ab -n 1000 -c 10 http://localhost:8092/api/health
```

### Stress Testing

```bash
# Monitor during stress
docker stats

# Watch logs
docker-compose logs -f

# Check Grafana dashboards
open http://localhost:3000
```

---

## 🔐 Security Testing

### Before Production

- [ ] Change default passwords
- [ ] Configure HTTPS
- [ ] Set up rate limiting
- [ ] Enable CORS properly
- [ ] Review environment variables
- [ ] Test authentication
- [ ] Validate input sanitization
- [ ] Check for SQL injection
- [ ] Test XSS protection
- [ ] Verify CSRF protection

---

## 📋 Testing Checklist

### Core Functionality
- [ ] Backend services start
- [ ] Health checks pass
- [ ] Database accessible
- [ ] API responds correctly
- [ ] Real-time updates work
- [ ] File uploads work
- [ ] Authentication works

### Client Apps
- [ ] Desktop builds and runs
- [ ] Web builds and runs
- [ ] Android builds and runs
- [ ] iOS builds and runs
- [ ] All platforms connect to backend
- [ ] Real-time sync works on all

### Monitoring
- [ ] Grafana accessible
- [ ] Metrics collecting
- [ ] Logs aggregating
- [ ] Alerts configured
- [ ] Dashboards showing data

### Performance
- [ ] Response times acceptable
- [ ] Memory usage normal
- [ ] CPU usage reasonable
- [ ] No memory leaks
- [ ] Real-time latency low

---

## 🎯 Quick Commands Reference

```bash
# Start/Stop
just backend          # Start dev stack
just stop             # Stop everything
just restart          # Restart backend

# Testing
./test-stack.sh       # Run test suite
curl localhost:8092/api/health  # Quick health check

# Apps
just desktop          # Run desktop
just web              # Run web
just android-studio   # Open Android Studio
just ios              # Open Xcode

# Monitoring
open http://localhost:3000   # Grafana
open http://localhost:9090   # Prometheus
docker-compose logs -f       # View logs
docker stats                 # Resource usage

# Database
open http://localhost:8092/_/  # Admin UI
docker exec -it bside-redis redis-cli  # Redis CLI

# Cleanup
just stop                    # Stop services
docker-compose down -v       # Remove volumes
./gradlew clean              # Clean builds
```

---

## 📞 Getting Help

- **Logs**: `docker-compose logs -f`
- **Health**: `curl localhost:8092/api/health`
- **Admin UI**: http://localhost:8092/_/
- **Monitoring**: http://localhost:3000
- **Test Script**: `./test-stack.sh`

---

*Last Updated: 2026-01-31*

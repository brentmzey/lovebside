# 🚀 BSIDE MVP - QUICK START GUIDE

## Current Status: 85% Ready for MVP

### ✅ What's Working Right Now
- Complete backend stack (PocketBase + Ktor + Nginx + Redis)
- Smart routing and load balancing
- Repository layer with offline support
- Basic UI screens (Auth, Profile, Messaging, Discovery)
- Real-time messaging infrastructure
- Optimistic locking for race condition prevention

### 🎯 To Complete MVP (Estimated: 2-3 weeks)

#### Week 1: Core Features
1. **Profile Management** (3 days)
   - Photo upload with CDN integration
   - Profile edit screen polish
   - Profile preview

2. **Matching System** (2 days)
   - Implement affinity algorithm
   - Swipe UI with animations
   - Match notification

#### Week 2: Polish & Test
3. **Messaging Enhancements** (2 days)
   - Typing indicators
   - Read receipts UI
   - Message reactions

4. **Testing** (3 days)
   - Integration tests
   - Load testing
   - Bug fixes

#### Week 3: Deploy
5. **Deployment** (2 days)
   - CDN setup (S3 + CloudFront)
   - Production environment
   - Monitoring setup

## 🏃 Quick Commands

### Start Development Stack
```bash
# Start all services
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f

# Access services
open http://localhost:8082        # Nginx
open http://localhost:8092/_/     # PocketBase Admin
open http://localhost:7817        # GoAccess Analytics
```

### Build & Run App
```bash
# Android
./gradlew composeApp:installDebug

# iOS (requires Mac)
cd iosApp && pod install
open iosApp.xcworkspace

# Desktop
./gradlew composeApp:run

# Web
./gradlew composeApp:jsBrowserDevelopmentRun
```

### Run Tests
```bash
# All tests
./gradlew test

# Specific module
./gradlew shared:test
./gradlew composeApp:test

# Integration tests
./gradlew shared:jvmTest
```

## 📁 Key Files to Know

### Backend
- `pocketbase/pb_migrations/` - Database schema
- `nginx/nginx.conf` - Routing configuration
- `server/src/` - Ktor backend (future)
- `docker-compose.yml` - Stack orchestration

### Frontend
- `composeApp/src/commonMain/kotlin/love/bside/app/`
  - `ui/screens/` - All screens
  - `ui/components/` - Reusable components
  - `ui/theme/` - Design system
  - `data/` - Data models (create this)

### Shared
- `shared/src/commonMain/kotlin/love/bside/app/`
  - `data/repository/` - Data access layer
  - `domain/models/` - Business models
  - `domain/usecase/` - Business logic

## 🎨 Design System Reference

### Colors
```kotlin
Primary: #FF6B9D (Coral Pink)
Secondary: #9B6BFF (Purple)
Success: #4CAF50
Error: #EF5350
```

### Spacing (8dp grid)
```kotlin
Small: 8dp
Medium: 16dp
Large: 24dp
XLarge: 32dp
```

### Components
- Min touch target: 48dp
- Card radius: 12-16dp
- Button height: 48dp

## 🐛 Common Issues & Solutions

### PocketBase not starting
```bash
# Reset PocketBase data
rm -rf pocketbase/pb_data/*
docker-compose restart pocketbase
```

### Gradle build fails
```bash
# Clean build
./gradlew clean build

# Update dependencies
./gradlew --refresh-dependencies
```

### iOS build fails
```bash
cd iosApp
pod deintegrate
pod install
```

## 📊 Health Checks

```bash
# Check all services
curl http://localhost:8082/health

# PocketBase
curl http://localhost:8092/api/health

# Redis
docker exec -it bside-redis redis-cli ping
```

## 🔑 Environment Variables

Create `.env` file:
```bash
# PocketBase
POCKETBASE_ADMIN_EMAIL=admin@bside.love
POCKETBASE_ADMIN_PASSWORD=your-secure-password

# CDN (future)
CDN_ENABLED=false
AWS_REGION=us-east-1
AWS_S3_BUCKET=bside-media
AWS_ACCESS_KEY_ID=your-key
AWS_SECRET_ACCESS_KEY=your-secret
```

## 📈 Performance Targets

- API Response: < 200ms (p95)
- Message Send: < 500ms
- Image Upload: < 3s (1MB)
- App Launch: < 2s
- Real-time Latency: < 100ms

## 🚀 Ready to Code!

1. Start Docker stack: `docker-compose up -d`
2. Open project in IDE
3. Run app: `./gradlew composeApp:run`
4. Start coding! 🎉

**Next task: Implement Profile Edit Screen**


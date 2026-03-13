# 🚀 Bside Development Quick Start

## Prerequisites
- Docker & Docker Compose (or OrbStack)
- Bun or Node.js 18+ (for PocketBase hooks)
- Gradle 8+ & JDK 17+
- Xcode (for iOS, Mac only)
- Android Studio (for Android)

---

## 1. Start Backend Services (5 minutes)

```bash
# Clone and enter directory
cd bside

# Start all services
docker-compose up -d

# Verify services are running
docker-compose ps

# Check health
curl http://localhost:8082/health  # Should return "healthy"
```

**Services Started**:
- ✅ Nginx (port 8082) - Smart load balancer
- ✅ PocketBase (port 8092) - Database & real-time
- ✅ Ktor (port 8081) - Business logic
- ✅ Redis (port 6379) - Caching & jobs
- ✅ GoAccess (port 7817) - Analytics

---

## 2. Initialize Database (2 minutes)

```bash
# Access PocketBase admin
open http://localhost:8092/_/

# Create admin account (first time only)
# Email: admin@bside.love
# Password: (your secure password)

# Migrations run automatically on startup!
# Check logs to verify:
docker-compose logs pocketbase | grep migration
```

---

## 3. Test API (1 minute)

```bash
# Create a test user
curl -X POST http://localhost:8082/api/pb/collections/users/records \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@bside.love",
    "password": "SecurePass123!",
    "passwordConfirm": "SecurePass123!",
    "name": "Test User"
  }'

# Test auth
curl -X POST http://localhost:8082/api/pb/collections/users/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{
    "identity": "test@bside.love",
    "password": "SecurePass123!"
  }'
```

---

## 4. Build Frontend (3 minutes)

### Android
```bash
./gradlew :composeApp:assembleDebug

# Install on device/emulator
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### iOS (Mac only)
```bash
cd iosApp
xcodebuild -scheme iosApp -configuration Debug -sdk iphonesimulator
open iosApp.xcworkspace
# Press Cmd+R to run
```

### Web
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
# Opens browser automatically at http://localhost:8080
```

---

## 5. Use UI Components

```kotlin
import love.bside.app.ui.components.*
import love.bside.app.ui.theme.BsideTheme

@Composable
fun MyScreen() {
    BsideTheme {
        AdaptiveContainer {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(BsideSpacing.medium)
            ) {
                // Email input
                BsideEmailField(
                    value = email,
                    onValueChange = { email = it },
                    isError = emailError != null,
                    errorText = emailError
                )
                
                MediumSpacer()
                
                // Password input
                BsidePasswordField(
                    value = password,
                    onValueChange = { password = it },
                    onDone = { onLoginClick() }
                )
                
                LargeSpacer()
                
                // Primary button
                BsidePrimaryButton(
                    text = "Sign In",
                    onClick = { onLoginClick() },
                    loading = isLoading
                )
                
                // Secondary button
                BsideSecondaryButton(
                    text = "Create Account",
                    onClick = { onSignUpClick() }
                )
            }
        }
    }
}
```

---

## 6. Connect to Backend

```kotlin
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.realtime.Realtime

// Initialize PocketBase client (using Supabase SDK as example)
val pocketbase = createSupabaseClient(
    supabaseUrl = "http://localhost:8092",
    supabaseKey = "your-anon-key"
) {
    install(GoTrue)
    install(Realtime)
}

// Sign in
val user = pocketbase.auth.signInWith(Email) {
    email = "test@bside.love"
    password = "SecurePass123!"
}

// Fetch profile
val profile = pocketbase
    .from("s_profiles")
    .select()
    .eq("user", user.id)
    .single()

// Subscribe to real-time updates
val channel = pocketbase.realtime.channel("messages")
channel.on<Message>("INSERT") { message ->
    println("New message: ${message.content}")
}
channel.subscribe()
```

---

## 7. Run Tests

```bash
# All tests
./gradlew test

# Specific module
./gradlew :composeApp:testDebugUnitTest

# With coverage
./gradlew test jacocoTestReport
```

---

## Common Tasks

### View Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f pocketbase
docker-compose logs -f nginx
```

### Restart Services
```bash
# Restart all
docker-compose restart

# Restart specific
docker-compose restart pocketbase
```

### Stop Services
```bash
docker-compose down

# Stop and remove volumes
docker-compose down -v
```

### Run Migrations
```bash
cd pocketbase
npm run migrate

# Or manually
docker-compose exec pocketbase ./pocketbase migrate up
```

### Access Databases
```bash
# PocketBase SQLite
docker-compose exec pocketbase sqlite3 /pb_data/data.db

# Redis CLI
docker-compose exec redis redis-cli
```

---

## Troubleshooting

### Port Already in Use
```bash
# Find process using port
lsof -i :8082

# Kill process
kill -9 <PID>
```

### Database Locked
```bash
# Restart PocketBase
docker-compose restart pocketbase

# Or reset database (⚠️ loses data)
docker-compose down -v
docker-compose up -d
```

### Frontend Build Fails
```bash
# Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies
```

### Nginx 502 Bad Gateway
```bash
# Check upstream services
docker-compose ps

# Restart Nginx
docker-compose restart nginx

# Check Nginx config
docker-compose exec nginx nginx -t
```

---

## Performance Tips

### Development Mode
- Use `docker-compose up` (without `-d`) to see logs live
- Enable hot reload for frontend
- Use PocketBase admin UI for quick data inspection

### Production Mode
- Use `docker-compose.production.yml`
- Enable Nginx caching
- Use CDN for static assets
- Monitor with GoAccess

---

## Next Steps

1. **Build Auth Flow** - Use `BsideEmailField` + `BsidePasswordField`
2. **Create Profile Screen** - Use `BsideTextField` + `BsideCard`
3. **Add Messaging** - Use `MessageCard` + real-time subscriptions
4. **Implement Matching** - Background jobs + scoring algorithms

---

## Resources

- **API Docs**: http://localhost:8092/_/
- **Analytics**: http://localhost:7817
- **Component Library**: `composeApp/src/commonMain/kotlin/love/bside/app/ui/components/`
- **Design System**: `composeApp/src/commonMain/kotlin/love/bside/app/ui/theme/`

---

**Happy Coding!** 🎉

For detailed architecture, see `IMPLEMENTATION_SUMMARY_MVP.md`  
For deployment, see `MVP_DELIVERY_REPORT.md`

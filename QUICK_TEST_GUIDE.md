# 🚀 Quick Test Guide - Get Started in 5 Minutes

## Prerequisites
- Docker and Docker Compose installed
- Ports available: 8082, 8081, 8092, 6379, 7817

## Step 1: Start the Stack (2 minutes)

```bash
./start-stack.sh
```

Wait for all services to show as healthy. You'll see:
```
✅ Redis is healthy!
✅ PocketBase is healthy!
✅ Backend Server is healthy!
✅ Nginx is healthy!
```

## Step 2: Run Integration Tests (1 minute)

```bash
./test-stack.sh
```

You should see:
```
🎉 ALL TESTS PASSED!
✅ Your Bside stack is ready to use!
```

## Step 3: Verify in Browser (1 minute)

Open these URLs to verify:

1. **Main App Health**: http://localhost:8082/health
   - Should return: `healthy`

2. **PocketBase Admin**: http://localhost:8092/_/
   - Login with: `tester_admin@bside.love` / `password123`
   - You should see all 13 collections

3. **GoAccess Dashboard**: http://localhost:7817
   - Real-time analytics dashboard

## Step 4: Test API Endpoints (1 minute)

```bash
# Test health
curl http://localhost:8082/health

# Test PocketBase collections
curl http://localhost:8082/api/pb/collections

# Test backend
curl http://localhost:8081/health
```

## Step 5: Run the Mobile App

### iOS
```bash
cd iosApp
open iosApp.xcodeproj
# Press Run in Xcode
```

### Android
```bash
./gradlew installDebug
```

### Web
```bash
./gradlew wasmJsBrowserRun
```

## Complete User Journey Test

1. **Sign Up**
   - Open the app
   - Click "Sign Up"
   - Enter email and password
   - Verify account created in PocketBase admin

2. **Proust Questionnaire**
   - Complete the questionnaire
   - Verify responses in `t_user_questionnaire_responses` collection

3. **Create Profile**
   - Add profile photo (optional)
   - Fill in bio and preferences
   - Verify in `s_profiles` collection

4. **Start Messaging**
   - Navigate to Messages
   - Start a new conversation
   - Send a message
   - Verify in `m_messages` collection

5. **Test Real-Time Features**
   - Open two instances of the app (different users)
   - Send messages back and forth
   - Test typing indicators
   - React to messages (❤️, 👍)
   - Try @mentions
   - Create a poll

## Troubleshooting

### Services won't start
```bash
# Stop everything and clean up
docker-compose down -v

# Restart
./start-stack.sh
```

### Port already in use
```bash
# Check what's using the port
lsof -i :8082

# Kill the process or change ports in docker-compose.yml
```

### Migrations not running
```bash
# Check PocketBase logs
docker-compose logs pocketbase

# Migrations run automatically on startup
```

### Can't connect from mobile app
```bash
# Make sure you're using the right URL
# iOS Simulator: http://localhost:8082
# Android Emulator: http://10.0.2.2:8082
# Real Device: http://YOUR_COMPUTER_IP:8082
```

## View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f nginx
docker-compose logs -f pocketbase
docker-compose logs -f server
```

## Stop the Stack

```bash
# Stop but keep data
docker-compose down

# Stop and delete all data (fresh start)
docker-compose down -v
```

## Success Criteria

✅ All integration tests pass
✅ Can access all URLs
✅ Can sign up a user
✅ Can complete questionnaire
✅ Can send messages
✅ Real-time updates work
✅ Typing indicators show
✅ Read receipts appear

## Next Steps

Once everything works:
1. Test with multiple users
2. Test file uploads
3. Monitor performance with GoAccess
4. Review logs for any errors
5. Start implementing matching algorithms!

---

**Need help?** Check the full docs:
- `STACK_READY.md` - Complete stack documentation
- `IMPLEMENTATION_COMPLETE_MVP.md` - Full feature list
- `docker-compose.yml` - Service configuration
- `nginx/nginx.conf` - Routing configuration

---
title: "Quick Start"
excerpt: "Get B-Side running in under 5 minutes"
category: "getting-started"
slug: "quick-start"
order: 2
---

# Quick Start Guide

Get B-Side up and running on your local machine in under 5 minutes.

## Prerequisites

Before you begin, ensure you have:

- **JDK 17+** (Eclipse Temurin 21 recommended)
- **Docker & Docker Compose** (for backend services)
- **Just** (command runner) - [Installation guide](https://github.com/casey/just)
- **Node.js 20+** (for Web target)

### Installing Just

[block:code]
{
  "codes": [
    {
      "code": "brew install just",
      "language": "shell",
      "name": "macOS"
    },
    {
      "code": "choco install just",
      "language": "shell",
      "name": "Windows"
    },
    {
      "code": "cargo install just",
      "language": "shell",
      "name": "Linux/Cargo"
    }
  ]
}
[/block]

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/bside.git
cd bside
```

### 2. Start the Backend

The easiest way to start all backend services:

```bash
just backend
```

This will:
1. Build the Ktor server JAR
2. Start Docker containers for:
   - PocketBase (Database): `http://localhost:8092`
   - Ktor API Server: `http://localhost:8081`
   - Redis (Cache): `localhost:6379`

Wait for this message:
```
✅ Backend services are running!
  PocketBase:      http://localhost:8092
  Ktor API:        http://localhost:8081
```

### 3. Run a Client

Choose your platform:

[block:code]
{
  "codes": [
    {
      "code": "just desktop",
      "language": "shell",
      "name": "Desktop"
    },
    {
      "code": "just web",
      "language": "shell",
      "name": "Web"
    },
    {
      "code": "just android-studio",
      "language": "shell",
      "name": "Android"
    },
    {
      "code": "just ios",
      "language": "shell",
      "name": "iOS"
    }
  ]
}
[/block]

## Verify Installation

### Check Backend Health

```bash
curl http://localhost:8092/api/health
```

Expected response:
```json
{
  "message": "API is healthy.",
  "code": 200,
  "data": {}
}
```

### Access Admin Panel

Open PocketBase admin panel:
```
http://localhost:8092/_/
```

**Default credentials:**
- Email: `tester_admin@bside.love`
- Password: `password123`

## First Login

When you launch a client app:

1. **Sign Up**: Create a new account or use test credentials
2. **Explore**: Send messages, try reactions, typing indicators
3. **Test Real-Time**: Open multiple clients to see live updates

### Test Accounts

Pre-configured test accounts for development:

| Email | Password | Role |
|-------|----------|------|
| `tester_admin@bside.love` | `password123` | Admin |
| `alice@test.com` | `test123` | User |
| `bob@test.com` | `test123` | User |

## What's Running?

After successful startup, you'll have:

```
┌─────────────────────────────────┐
│  Your Local Development Stack   │
├─────────────────────────────────┤
│                                 │
│  🐳 Docker Containers:          │
│    • PocketBase (:8092)         │
│    • Ktor Server (:8081)        │
│    • Redis (:6379)              │
│    • Nginx (:80)                │
│                                 │
│  📱 Your Client App             │
│    (connecting to :8092)        │
│                                 │
└─────────────────────────────────┘
```

## Common Commands

[block:parameters]
{
  "data": {
    "h-0": "Command",
    "h-1": "Description",
    "0-0": "`just backend`",
    "0-1": "Start backend services",
    "1-0": "`just stop`",
    "1-1": "Stop all services",
    "2-0": "`just restart`",
    "2-1": "Restart backend",
    "3-0": "`just desktop`",
    "3-1": "Run desktop app",
    "4-0": "`just web`",
    "4-1": "Run web app",
    "5-0": "`just migrate`",
    "5-1": "Run database migrations"
  },
  "cols": 2,
  "rows": 6
}
[/block]

## Troubleshooting

### Backend won't start

```bash
just stop
just backend
```

### Port already in use

Check what's using the port:
```bash
lsof -i :8092
lsof -i :8081
```

Kill the process or change ports in `docker-compose.yml`.

### Build errors

Clean and rebuild:
```bash
./gradlew clean build
```

### Docker issues

Reset Docker state:
```bash
docker-compose down -v
docker system prune -a
just backend
```

## Next Steps

Now that you have B-Side running:

1. 📖 [Explore the Architecture](../architecture/overview)
2. 🛠️ [Learn about Building](../guides/building)
3. 🧪 [Run Tests](../guides/testing)
4. 🚀 [Deploy to Production](../guides/deployment)

---

> ❗️ Development Mode
> 
> The quick start uses development configurations with relaxed security. See [Production Deployment](../guides/deployment) before going live.

> 📘 Need Help?
> 
> Check our [Troubleshooting Guide](../reference/troubleshooting) or [open an issue](https://github.com/your-org/bside/issues).

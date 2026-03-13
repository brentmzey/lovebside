# Full Stack Local Development Guide

**Last Updated:** 2026-01-31  
**Status:** ✅ Production-Ready

## Overview

This guide provides complete instructions for running the entire Bside application stack locally, mirroring production infrastructure.

## 📋 Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Prerequisites](#prerequisites)
3. [Quick Start](#quick-start)
4. [Service Details](#service-details)
5. [Monitoring & Observability](#monitoring--observability)
6. [Database Management](#database-management)
7. [Development Workflow](#development-workflow)
8. [Troubleshooting](#troubleshooting)
9. [Production Deployment](#production-deployment)

---

## Architecture Overview

### Stack Components

```
┌─────────────────────────────────────────────────────────────┐
│                     Client Applications                      │
│              (Web, iOS, Android, Desktop)                   │
└────────────────────────┬────────────────────────────────────┘
                         │
                    ┌────▼────┐
                    │  Nginx  │ (Reverse Proxy & Load Balancer)
                    │  :8082  │
                    └────┬────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │  Ktor   │    │Pocket   │    │ Static  │
    │ Backend │    │  Base   │    │  Files  │
    │  :8081  │    │  :8092  │    │         │
    └────┬────┘    └────┬────┘    └─────────┘
         │              │
         │         ┌────▼────┐
         └────────►│  Redis  │ (Cache & Queue)
                   │  :6379  │
                   └─────────┘

┌─────────────────────────────────────────────────────────────┐
│                   Monitoring & Observability                 │
├─────────────┬─────────────┬─────────────┬──────────────────┤
│  Prometheus │   Grafana   │  GoAccess   │ Redis Commander  │
│    :9090    │    :3000    │    :7817    │      :8083       │
└─────────────┴─────────────┴─────────────┴──────────────────┘
```

### Technology Stack

| Component | Technology | Purpose |
|-----------|-----------|---------|
| **Frontend** | Compose Multiplatform | Cross-platform UI |
| **Backend API** | Ktor (Kotlin) | Business logic, jobs, API |
| **Database** | PocketBase (SQLite) | Data persistence, auth, realtime |
| **Cache/Queue** | Redis | Session store, job queue, caching |
| **Reverse Proxy** | Nginx | Load balancing, SSL, routing |
| **Metrics** | Prometheus | Time-series metrics collection |
| **Visualization** | Grafana | Dashboards and alerting |
| **Log Analysis** | GoAccess | Real-time log analytics |

---

## Prerequisites

### Required Software

- **Docker**: >= 20.10
- **Docker Compose**: >= 2.0 (or `docker compose` plugin)
- **Java JDK**: >= 17 (for building backend)
- **Gradle**: 8.x (wrapper included)
- **Node.js**: >= 18.x (for PocketBase migrations)

### Optional Tools

- **Make** or **Just**: Task automation
- **curl**: API testing
- **jq**: JSON processing
- **httpie**: Better HTTP client

### System Requirements

- **RAM**: Minimum 4GB, Recommended 8GB+
- **Disk**: 10GB free space (for Docker images and data)
- **Ports**: Ensure these ports are available:
  - 3000 (Grafana)
  - 6379 (Redis)
  - 7817 (GoAccess)
  - 8081 (Backend)
  - 8082 (Nginx)
  - 8083 (Redis UI)
  - 8092 (PocketBase)
  - 9090 (Prometheus)

---

## Quick Start

### 1. Clone and Setup

```bash
cd ~/bside
cp .env.example .env
# Edit .env with your configuration if needed
```

### 2. Start Full Stack

**Option A: Full Stack with Monitoring**
```bash
./start-full-stack.sh
```

**Option B: Basic Stack (No Monitoring)**
```bash
./start-full-stack.sh --basic
```

**Option C: Production Stack**
```bash
./start-full-stack.sh --production
```

### 3. Verify Services

Open your browser and check:

- ✅ Main App: http://localhost:8082
- ✅ Backend API: http://localhost:8081/health
- ✅ PocketBase: http://localhost:8092/api/health
- ✅ Grafana: http://localhost:3000 (admin/admin)
- ✅ Prometheus: http://localhost:9090
- ✅ GoAccess: http://localhost:7817

### 4. Test API

```bash
# Health check
curl http://localhost:8082/health

# PocketBase health
curl http://localhost:8082/api/pb/health

# Backend API health
curl http://localhost:8081/health
```

---

## Service Details

### PocketBase (Database & Auth)

**URL:** http://localhost:8092  
**Admin Panel:** http://localhost:8092/_/  
**Credentials:** `tester_admin@bside.love` / `password123`

**Key Features:**
- RESTful API for all collections
- Real-time subscriptions via SSE
- Built-in authentication & authorization
- File storage and uploads
- Database migrations via `pb_migrations/`

**Database Location:**
```
./pocketbase/pb_data/data.db
```

**Migrations:**
```bash
# Migrations auto-run on startup
# Located in: ./pocketbase/pb_migrations/
```

**Manual Migration:**
```bash
docker-compose exec pocketbase ./pocketbase migrate up
```

### Ktor Backend Server

**URL:** http://localhost:8081  
**Metrics:** http://localhost:8081/metrics

**Key Features:**
- Business logic and orchestration
- Background job processing
- External API integrations
- Caching via Redis
- Rate limiting

**Build Backend:**
```bash
./gradlew :server:clean :server:shadowJar
```

**View Logs:**
```bash
docker-compose logs -f server
# or
tail -f ./logs/server.log
```

### Redis (Cache & Queue)

**URL:** localhost:6379  
**UI:** http://localhost:8083 (Redis Commander)

**Usage:**
- Session storage
- Job queue (BullMQ)
- API response caching
- Distributed locking

**CLI Access:**
```bash
docker-compose exec redis redis-cli

# Common commands:
KEYS *                 # List all keys
GET key                # Get value
INFO                   # Server info
MONITOR                # Watch commands in real-time
```

### Nginx (Reverse Proxy)

**URL:** http://localhost:8082

**Routing:**
```
/                      → Backend Server
/api/v1/*             → Backend Server
/api/pb/*             → PocketBase
/_/*                  → PocketBase Admin
/health               → Health Check Aggregation
```

**Configuration:**
```
./nginx/nginx.conf
```

**View Access Logs:**
```bash
docker-compose exec nginx tail -f /var/log/nginx/access.log
```

---

## Monitoring & Observability

### Prometheus (Metrics Collection)

**URL:** http://localhost:9090

**Metrics Collected:**
- Backend API response times
- Redis cache hit/miss rates
- Nginx request counts
- Docker container stats
- Custom business metrics

**Query Examples:**
```promql
# Backend request rate
rate(http_requests_total[5m])

# Redis cache hit ratio
redis_keyspace_hits / (redis_keyspace_hits + redis_keyspace_misses)

# 95th percentile response time
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))
```

### Grafana (Visualization)

**URL:** http://localhost:3000  
**Default Login:** admin / admin

**Pre-configured Dashboards:**
1. **System Overview** - All services health
2. **API Performance** - Request rates, latencies
3. **Redis Metrics** - Cache performance
4. **Database Stats** - PocketBase metrics

**Adding Custom Dashboard:**
```bash
# Place JSON in:
./monitoring/grafana/dashboards/custom-dashboard.json
```

### GoAccess (Log Analysis)

**URL:** http://localhost:7817

**Real-time Metrics:**
- Requests per second
- Response time distribution
- Top URLs and endpoints
- Geographic distribution
- User agents and browsers
- Status code distribution

### Redis Commander (Cache UI)

**URL:** http://localhost:8083

**Features:**
- Browse all keys
- View key values and TTL
- Delete keys
- Execute Redis commands
- Memory usage analysis

---

## Database Management

### Schema Migrations

**Location:** `./pocketbase/pb_migrations/`

**Create New Migration:**
```bash
cd pocketbase
npm run migrate:create <migration_name>
```

**Apply Migrations:**
```bash
# Auto-applied on startup, or manually:
docker-compose exec pocketbase ./pocketbase migrate up
```

**Rollback Migration:**
```bash
docker-compose exec pocketbase ./pocketbase migrate down 1
```

### Database Backup

**Manual Backup:**
```bash
# Stop services first
docker-compose stop pocketbase

# Backup database
cp pocketbase/pb_data/data.db pocketbase/pb_data/data.db.backup_$(date +%Y%m%d_%H%M%S)

# Restart
docker-compose start pocketbase
```

**Automated Backup Script:**
```bash
#!/bin/bash
# Add to cron for automated backups
docker-compose exec pocketbase ./pocketbase backup create
```

### Database Optimization

**Rebuild Indexes:**
```bash
docker-compose exec pocketbase ./pocketbase vacuum
```

**Check Database Size:**
```bash
du -h pocketbase/pb_data/data.db
```

---

## Development Workflow

### Making Code Changes

**Backend (Ktor) Changes:**
```bash
# 1. Make changes to code in ./server/src/
# 2. Rebuild JAR
./gradlew :server:shadowJar

# 3. Restart container
docker-compose restart server

# 4. Or rebuild image
docker-compose up -d --build server
```

**PocketBase Schema Changes:**
```bash
# 1. Create migration
cd pocketbase && npm run migrate:create <name>

# 2. Edit migration file
# ./pocketbase/pb_migrations/XXXXXXXXXX_<name>.js

# 3. Restart PocketBase (auto-applies)
docker-compose restart pocketbase
```

**Nginx Configuration Changes:**
```bash
# 1. Edit ./nginx/nginx.conf
# 2. Test config
docker-compose exec nginx nginx -t

# 3. Reload
docker-compose exec nginx nginx -s reload
```

### Hot Reload Development

**Backend Hot Reload:**
```bash
# Run backend locally (not in Docker)
./gradlew :server:run

# Points to Docker PocketBase and Redis
```

### Running Tests

**Backend Tests:**
```bash
./gradlew :server:test
```

**Integration Tests:**
```bash
./gradlew :server:integrationTest
```

**Load Tests:**
```bash
# Using Apache Bench
ab -n 1000 -c 10 http://localhost:8082/api/v1/health

# Using k6
k6 run scripts/load-test.js
```

---

## Troubleshooting

### Common Issues

#### Port Already in Use

```bash
# Find process using port
lsof -i :8082

# Kill process
kill -9 <PID>

# Or change port in docker-compose.yml
```

#### Container Won't Start

```bash
# Check logs
docker-compose logs <service>

# Check container status
docker-compose ps

# Rebuild from scratch
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

#### Database Locked

```bash
# Stop all services
docker-compose down

# Remove lock file
rm pocketbase/pb_data/data.db-wal
rm pocketbase/pb_data/data.db-shm

# Restart
docker-compose up -d
```

#### Redis Connection Refused

```bash
# Check Redis is running
docker-compose ps redis

# Restart Redis
docker-compose restart redis

# Check logs
docker-compose logs redis
```

### Debug Mode

**Enable Debug Logging:**
```bash
# Edit .env
DEBUG=true
LOG_LEVEL=debug

# Restart services
docker-compose restart
```

**View All Logs:**
```bash
docker-compose logs -f --tail=100
```

### Performance Issues

**Check Resource Usage:**
```bash
docker stats

# Or use docker-compose
docker-compose stats
```

**Clear Redis Cache:**
```bash
docker-compose exec redis redis-cli FLUSHALL
```

**Optimize Database:**
```bash
docker-compose exec pocketbase ./pocketbase vacuum
docker-compose exec pocketbase ./pocketbase optimize
```

---

## Production Deployment

### Environment Configuration

**Production `.env`:**
```bash
# Database
PB_PUBLIC_URL=https://api.bside.app
POCKETBASE_ADMIN_EMAIL=admin@bside.app
POCKETBASE_ADMIN_PASSWORD=<strong-password>

# CDN
CDN_ENABLED=true
CDN_BASE_URL=https://cdn.bside.app

# AWS S3
AWS_REGION=us-east-1
AWS_S3_BUCKET=bside-media-prod
AWS_ACCESS_KEY_ID=<key>
AWS_SECRET_ACCESS_KEY=<secret>

# Redis
REDIS_URL=redis://redis:6379
REDIS_PASSWORD=<strong-password>
```

### SSL/TLS Configuration

**Nginx SSL Setup:**
```bash
# Generate certificates (or use Let's Encrypt)
./scripts/generate-ssl-certs.sh

# Update nginx/nginx.conf with SSL config
# Restart Nginx
docker-compose restart nginx
```

### Scaling

**Horizontal Scaling:**
```bash
# Scale backend servers
docker-compose up -d --scale server=3

# Nginx will load balance automatically
```

**Use Production Compose:**
```bash
docker-compose -f docker-compose.production.yml up -d
```

This includes:
- 4x PocketBase instances (1 master, 3 replicas)
- 2x Backend servers
- Redis cluster
- Enhanced monitoring

### Monitoring in Production

**Health Check Endpoint:**
```bash
curl https://api.bside.app/health
```

**Alerts Setup:**
- Configure Prometheus AlertManager
- Set up PagerDuty/Slack integrations
- Define SLOs and error budgets

---

## Additional Resources

- [PocketBase Documentation](https://pocketbase.io/docs/)
- [Ktor Documentation](https://ktor.io/docs/)
- [Docker Compose Reference](https://docs.docker.com/compose/)
- [Prometheus Query Guide](https://prometheus.io/docs/prometheus/latest/querying/)
- [Grafana Dashboard Guide](https://grafana.com/docs/grafana/latest/dashboards/)

---

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review logs: `docker-compose logs <service>`
3. Contact the team on Slack: #bside-dev

---

**Last Reviewed:** 2026-01-31
**Maintained By:** Bside Engineering Team

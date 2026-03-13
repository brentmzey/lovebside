# 🚀 Bside Complete System - Quick Start Guide

## One Command to Rule Them All

```bash
./start-complete-system.sh development full
```

This launches **everything**:
- ✅ Backend API (Ktor)
- ✅ Database (PocketBase) 
- ✅ Cache (Redis)
- ✅ Reverse Proxy (Nginx)
- ✅ Metrics (Prometheus + Grafana)
- ✅ Logs (Loki)
- ✅ Traces (Tempo)
- ✅ Monitoring (cAdvisor, exporters)

## Access Your Stack

### Core Services
| Service | URL | Purpose |
|---------|-----|---------|
| **Main App** | http://localhost:8082 | Your application entry point |
| **Backend API** | http://localhost:8081 | Ktor REST API |
| **PocketBase** | http://localhost:8092 | Database + Admin UI |
| **Redis** | localhost:6379 | Cache & queues |

### Monitoring Stack
| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | `admin` / `admin` |
| **Prometheus** | http://localhost:9090 | - |
| **Loki** | http://localhost:3100 | - |
| **Tempo** | http://localhost:3200 | - |
| **AlertManager** | http://localhost:9093 | - |
| **cAdvisor** | http://localhost:8084 | - |

## Quick Commands

### Start/Stop
```bash
# Start everything
./start-complete-system.sh development full

# Start app only (no monitoring)
./start-complete-system.sh development app-only

# Start monitoring only
./start-complete-system.sh development observability-only

# Stop everything
docker-compose -f docker-compose.full.yml down
docker-compose -f docker-compose.observability.yml down
```

### View Logs
```bash
# All services
docker-compose -f docker-compose.full.yml logs -f

# Specific service
docker-compose -f docker-compose.full.yml logs -f server
docker-compose -f docker-compose.full.yml logs -f pocketbase
docker-compose -f docker-compose.full.yml logs -f nginx
```

### Health Checks
```bash
# Quick test all services
curl http://localhost:8082/health    # Main app
curl http://localhost:8081/health    # Backend
curl http://localhost:8092/api/health # PocketBase
```

### Restart Services
```bash
# Restart one service
docker-compose -f docker-compose.full.yml restart server

# Restart all
docker-compose -f docker-compose.full.yml restart
```

## View Metrics & Monitoring

### 1. Open Grafana
```bash
open http://localhost:3000
```
- Login: `admin` / `admin`
- Browse pre-configured dashboards
- View real-time metrics

### 2. Check Prometheus
```bash
open http://localhost:9090
```
- Query metrics directly
- View targets status
- Check alerting rules

### 3. View Container Stats
```bash
open http://localhost:8084
```
- Live container metrics
- Resource usage
- Performance stats

## Testing Real-Time Messaging

### 1. Check Backend Health
```bash
curl http://localhost:8081/health
```

### 2. Test PocketBase Connection
```bash
# Get collections
curl http://localhost:8092/api/collections

# Health check
curl http://localhost:8092/api/health
```

### 3. Monitor Message Traffic
- Open Grafana: http://localhost:3000
- Navigate to "Messaging Dashboard"
- Watch real-time metrics as you send messages

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   Nginx (8082)                      │
│              Reverse Proxy & Load Balancer          │
└───────────────┬─────────────────┬───────────────────┘
                │                 │
        ┌───────▼──────┐   ┌─────▼──────────┐
        │   Backend    │   │   PocketBase   │
        │  (Ktor)      │   │   (Database)   │
        │   :8081      │   │     :8092      │
        └──────┬───────┘   └────────────────┘
               │
        ┌──────▼───────┐
        │    Redis     │
        │  (Cache)     │
        │    :6379     │
        └──────────────┘

┌─────────────────────────────────────────────────────┐
│            Observability Stack                      │
├─────────────────────────────────────────────────────┤
│  Grafana (3000) ─── Prometheus (9090)               │
│       │                    │                        │
│       ├─── Loki (3100)     │                        │
│       └─── Tempo (3200)    │                        │
│                            │                        │
│         Metrics Exporters: │                        │
│         • Node (9100)      │                        │
│         • Redis (9121)     │                        │
│         • cAdvisor (8084)  │                        │
└─────────────────────────────────────────────────────┘
```

## Performance Monitoring

### What to Watch
1. **Message Latency** - Check in Grafana "Messaging" dashboard
2. **Database Queries** - PocketBase metrics in Grafana
3. **Memory Usage** - cAdvisor at http://localhost:8084
4. **Redis Performance** - Redis exporter metrics in Prometheus
5. **API Response Times** - Backend metrics endpoint

### Key Metrics
```bash
# Backend metrics
curl http://localhost:8081/metrics

# Redis metrics  
curl http://localhost:9121/metrics

# Node metrics
curl http://localhost:9100/metrics
```

## Troubleshooting

### Services Won't Start
```bash
# Clean up and retry
docker-compose -f docker-compose.full.yml down --volumes
docker-compose -f docker-compose.observability.yml down --volumes
./start-complete-system.sh development full
```

### Can't Access Grafana
```bash
# Check if running
docker ps | grep grafana

# Restart
docker-compose -f docker-compose.observability.yml restart grafana

# Check logs
docker-compose -f docker-compose.observability.yml logs grafana
```

### Backend Not Responding
```bash
# Check logs
docker-compose -f docker-compose.full.yml logs server

# Rebuild and restart
./gradlew :server:clean :server:shadowJar
docker-compose -f docker-compose.full.yml restart server
```

### PocketBase Issues
```bash
# Check logs
docker-compose -f docker-compose.full.yml logs pocketbase

# Reset database (DEV ONLY!)
rm -rf pocketbase/pb_data/data.db
docker-compose -f docker-compose.full.yml restart pocketbase
```

## Environment Variables

Key settings in `.env`:
```bash
# Backend
SERVER_PORT=8081
SERVER_HOST=0.0.0.0

# PocketBase
POCKETBASE_URL=http://pocketbase:8090

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# Monitoring
PROMETHEUS_PORT=9090
GRAFANA_PORT=3000
```

## Next Steps

### 1. Import Grafana Dashboards
```bash
# Coming soon - pre-built dashboards for:
# - Messaging metrics
# - API performance
# - Database stats
# - Redis cache
```

### 2. Configure Alerts
- Open Prometheus: http://localhost:9090
- Go to "Alerts" tab
- Configure thresholds

### 3. Test Load
```bash
# Run load test (coming soon)
./scripts/load-test.sh
```

### 4. Deploy to Production
See: `POCKETBASE_PRODUCTION_DEPLOYMENT_GUIDE.md`

## Media Storage (Coming Soon)

Current: Media stored in PocketBase database
Target: AWS S3 + CloudFront CDN

See: `.code-hq/stories/cdn-media-storage.md` for implementation plan

## Need Help?

- 📖 Full docs: `./readme-docs/`
- 🚀 Deployment: `POCKETBASE_PRODUCTION_DEPLOYMENT_GUIDE.md`
- 📊 Observability: `OBSERVABILITY_GUIDE.md`
- 🏗️ Architecture: `AWS_CDN_IMPLEMENTATION_GUIDE.md`

---

**Pro Tips:**
- Always check Grafana first when troubleshooting
- Use `docker-compose logs -f [service]` to follow real-time logs
- Monitor cAdvisor for resource usage
- Prometheus has all raw metrics - query directly when needed

**Happy Coding! 🎉**

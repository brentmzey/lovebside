# Bside Stack Quick Reference Card

**Last Updated:** 2026-01-31

---

## 🚀 Quick Start (30 seconds)

```bash
cd ~/bside
./start-full-stack.sh
# Wait 60 seconds
# Open http://localhost:8082
```

---

## 🌐 Service URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Main App** | http://localhost:8082 | - |
| **Backend API** | http://localhost:8081 | - |
| **PocketBase** | http://localhost:8092 | - |
| **PocketBase Admin** | http://localhost:8092/_/ | tester_admin@bside.love / password123 |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | - |
| **GoAccess** | http://localhost:7817 | - |
| **Redis UI** | http://localhost:8083 | - |

---

## 📡 API Endpoints

```bash
# Health checks
curl http://localhost:8082/health              # Nginx
curl http://localhost:8081/health              # Backend
curl http://localhost:8092/api/health          # PocketBase

# Backend API
curl http://localhost:8082/api/v1/*            # Backend routes

# PocketBase API
curl http://localhost:8082/api/pb/*            # Database API
curl http://localhost:8082/api/pb/files/*      # File uploads
```

---

## 🔧 Common Commands

### Start/Stop
```bash
./start-full-stack.sh                          # Start full stack
./start-full-stack.sh --basic                  # Start without monitoring
./start-full-stack.sh --stop                   # Stop everything
docker-compose down                            # Alternative stop
docker-compose down -v                         # Stop + delete volumes
```

### Logs
```bash
docker-compose logs -f                         # All services
docker-compose logs -f server                  # Backend only
docker-compose logs -f pocketbase              # Database only
docker-compose logs -f nginx                   # Proxy only
docker-compose logs --tail=100 server          # Last 100 lines
```

### Rebuild
```bash
./gradlew :server:shadowJar                    # Rebuild backend
docker-compose build server                    # Rebuild image
docker-compose up -d --build server            # Rebuild & restart
```

### Database
```bash
# Access database CLI
docker-compose exec pocketbase ./pocketbase admin

# Run migrations
docker-compose exec pocketbase ./pocketbase migrate up

# Backup database
docker-compose exec pocketbase ./pocketbase backup create

# View schema
sqlite3 pocketbase/pb_data/data.db ".schema"
```

### Redis
```bash
# Access Redis CLI
docker-compose exec redis redis-cli

# Common Redis commands
KEYS *                                         # List all keys
GET key                                        # Get value
FLUSHALL                                       # Clear all data
INFO                                           # Server stats
MONITOR                                        # Watch commands
```

### Container Management
```bash
docker-compose ps                              # List containers
docker-compose restart [service]               # Restart service
docker-compose stop [service]                  # Stop service
docker-compose up -d [service]                 # Start service
docker stats                                   # Resource usage
```

---

## 🐛 Troubleshooting

### Port Already in Use
```bash
lsof -i :8082                                  # Find process
kill -9 <PID>                                  # Kill process
```

### Container Won't Start
```bash
docker-compose logs <service>                  # Check logs
docker-compose down -v                         # Clean restart
docker-compose up -d --force-recreate          # Force rebuild
```

### Clear Cache
```bash
docker-compose exec redis redis-cli FLUSHALL   # Clear Redis
rm -rf pocketbase/pb_data/data.db-wal          # Clear DB lock
```

### Reset Everything
```bash
docker-compose down -v                         # Stop & delete volumes
rm -rf pocketbase/pb_data/*                    # Delete database
./start-full-stack.sh                          # Fresh start
```

---

## 📊 Monitoring Quick Access

### Prometheus Queries
```promql
# Request rate
rate(http_requests_total[5m])

# Error rate
rate(http_requests_total{status=~"5.."}[5m])

# Response time (95th percentile)
histogram_quantile(0.95, http_request_duration_seconds_bucket)

# Redis cache hit ratio
redis_keyspace_hits / (redis_keyspace_hits + redis_keyspace_misses)
```

### Grafana Dashboards
- **System Overview** - All services health and metrics
- **API Performance** - Request rates, latencies, errors
- **Redis Metrics** - Cache hit rates, memory usage
- **Database Stats** - Query times, connections

---

## 🔐 Environment Variables

**Key Variables in `.env`:**
```bash
PB_PUBLIC_URL=http://localhost:8090            # PocketBase URL
POCKETBASE_ADMIN_EMAIL=tester_admin@bside.love # Admin email
POCKETBASE_ADMIN_PASSWORD=password123          # Admin password
CDN_ENABLED=false                              # CDN toggle
AWS_REGION=us-east-1                           # AWS region
```

---

## 📝 File Locations

```
bside/
├── .env                                       # Environment config
├── docker-compose.yml                         # Basic stack
├── docker-compose.full.yml                    # Full stack + monitoring
├── docker-compose.production.yml              # Production config
├── start-full-stack.sh                        # Orchestration script
├── server/                                    # Backend code
│   ├── src/                                   # Kotlin source
│   ├── build.gradle.kts                       # Build config
│   └── Dockerfile                             # Docker image
├── pocketbase/                                # Database
│   ├── pb_data/                               # SQLite data
│   ├── pb_migrations/                         # Schema migrations
│   └── pb_hooks/                              # Custom logic
├── nginx/                                     # Reverse proxy
│   └── nginx.conf                             # Nginx config
└── monitoring/                                # Observability
    ├── prometheus.yml                         # Metrics config
    └── grafana/                               # Dashboard config
```

---

## 🧪 Testing

```bash
# Health checks
curl http://localhost:8082/health

# Backend API test
curl http://localhost:8081/health

# PocketBase test
curl http://localhost:8092/api/health

# Load test (requires Apache Bench)
ab -n 1000 -c 10 http://localhost:8082/health
```

---

## 📞 Support

**Logs:** `docker-compose logs -f`  
**Status:** `docker-compose ps`  
**Docs:** `cat .code-hq/FULL_STACK_GUIDE.md`  
**Team:** #bside-dev on Slack

---

## 🎯 Common Tasks Cheat Sheet

| Task | Command |
|------|---------|
| Start stack | `./start-full-stack.sh` |
| Stop stack | `docker-compose down` |
| View logs | `docker-compose logs -f` |
| Rebuild backend | `./gradlew :server:shadowJar` |
| Clear cache | `docker-compose exec redis redis-cli FLUSHALL` |
| Database backup | `cp pocketbase/pb_data/data.db data.db.backup` |
| Run migrations | `docker-compose exec pocketbase ./pocketbase migrate up` |
| View metrics | Open http://localhost:3000 |
| View logs viz | Open http://localhost:7817 |

---

**Quick Help:** `./start-full-stack.sh --help`

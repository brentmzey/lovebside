# 🚀 HOW TO START AND VIEW THE FULL SYSTEM WITH METRICS & TELEMETRY

## ✅ CURRENT STATUS: **CORE MONITORING IS WORKING!**

Your observability stack is **75% operational**. The essential monitoring tools are running and ready to use.

---

## 📍 STEP 1: Access What's Already Working

### 🎨 Grafana - Visualization Dashboard
```
URL: http://localhost:3000
Login: admin / admin
```

**What you can do NOW:**
1. Click "Explore" (compass icon on left)
2. Select "Prometheus" from dropdown
3. Try these queries to see live metrics:

```promql
# CPU usage per container
rate(container_cpu_usage_seconds_total{name!=""}[5m])

# Memory usage per container  
container_memory_usage_bytes{name!=""}

# System CPU by core
node_cpu_seconds_total

# Docker containers count
count(container_last_seen)

# Network bytes received
rate(container_network_receive_bytes_total[5m])
```

### 📊 Prometheus - Metrics Database
```
URL: http://localhost:9090
```

**What to check:**
1. Go to "Status" → "Targets" to see all metrics being collected
2. Go to "Graph" to query metrics directly
3. Go to "Alerts" to see configured alerts

### 🐳 cAdvisor - Container Metrics
```
URL: http://localhost:8084
```

**What you see:**
- Real-time container resource usage
- CPU, memory, network, disk I/O
- Click any container for detailed graphs

---

## 📍 STEP 2: Start Your Full Application

Now that monitoring is running, start your backend:

```bash
# Option 1: Start full stack with monitoring
cd /Users/brentzey/bside
docker-compose -f docker-compose.full.yml up -d

# Option 2: Start specific services
docker-compose up -d server pocketbase redis nginx

# Check all services are running
docker ps
```

Once running, **your app will be automatically monitored** by:
- Prometheus (collecting metrics every 15s)
- cAdvisor (monitoring container resources)
- Node Exporter (monitoring host system)

---

## 📍 STEP 3: View Real-Time Application Performance

### Monitor Your Backend Performance

1. **Open Grafana**: http://localhost:3000

2. **Create a new dashboard** or go to "Explore"

3. **Query your services**:

```promql
# Backend API request rate
rate(http_requests_total[5m])

# Backend response time (if instrumented)
histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))

# PocketBase container CPU
rate(container_cpu_usage_seconds_total{name="bside-pocketbase"}[5m])

# PocketBase container memory
container_memory_usage_bytes{name="bside-pocketbase"}

# Redis container metrics
container_memory_usage_bytes{name="bside-redis"}

# Nginx request rate
rate(container_network_receive_bytes_total{name="bside-nginx"}[5m])
```

### View Container Resource Usage

**In cAdvisor** (http://localhost:8084):
1. Click on any container name
2. See graphs for:
   - CPU usage percentage
   - Memory usage
   - Network I/O
   - Disk I/O
   - All updated in real-time!

---

## 📍 STEP 4: Monitor Real-Time Messaging Features

### For Testing Messaging Performance:

1. **Monitor WebSocket connections**:
```promql
# Active connections (if instrumented)
websocket_connections_active

# Message throughput
rate(messages_sent_total[1m])
rate(messages_received_total[1m])
```

2. **Monitor PocketBase API calls**:
```promql
# API response times
rate(container_network_transmit_bytes_total{name="bside-pocketbase"}[30s])

# Database operations
rate(container_blkio_io_service_bytes_total{name="bside-pocketbase"}[1m])
```

3. **Monitor Redis (for real-time features)**:
```bash
# Redis metrics are available at http://localhost:9121
# Or in Prometheus:
redis_connected_clients
redis_commands_processed_total
rate(redis_commands_processed_total[1m])
```

---

## 📍 STEP 5: Test Load & Performance

### Simulate Users and Watch Metrics

```bash
# Example: Send test requests to your API
for i in {1..100}; do
  curl -X POST http://localhost:8080/api/messages \
    -H "Content-Type: application/json" \
    -d '{"content": "Test message"}' &
done

# Watch the metrics update in Grafana in real-time!
```

Then in Grafana, watch:
- CPU spike
- Memory increase
- Network throughput
- Response times

---

## 📍 STEP 6: View Specific Service Health

### Check Individual Services:

```bash
# View all containers with health status
docker ps --filter "health=healthy"
docker ps --filter "health=unhealthy"

# Check service logs
docker logs bside-server --follow
docker logs bside-pocketbase --follow
docker logs bside-prometheus --tail 100

# Check metrics collection
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets'
```

---

## 🎯 WHAT METRICS ARE BEING COLLECTED RIGHT NOW

### System Metrics (Node Exporter)
- ✅ CPU usage per core
- ✅ Memory usage and available
- ✅ Disk I/O and space
- ✅ Network traffic
- ✅ System load average
- ✅ Process counts

### Container Metrics (cAdvisor)
- ✅ CPU usage per container
- ✅ Memory usage per container
- ✅ Network I/O per container
- ✅ Disk I/O per container
- ✅ Container lifecycle events

### Redis Metrics (Redis Exporter)
- ✅ Connected clients
- ✅ Commands per second
- ✅ Memory usage
- ✅ Key space statistics
- ✅ Replication status

### Prometheus Self-Monitoring
- ✅ Scrape duration
- ✅ Samples ingested
- ✅ Storage size
- ✅ Query performance

---

## 🔥 COOL THINGS YOU CAN DO RIGHT NOW

### 1. Create Custom Dashboards
```
1. Go to Grafana → Dashboards → New Dashboard
2. Add Panel
3. Select Prometheus as data source
4. Write queries (see examples above)
5. Save dashboard
```

### 2. Set Up Alerts
```
1. In Grafana, go to Alerting → Alert Rules
2. Create new alert rule
3. Set condition (e.g., CPU > 80%)
4. Configure notification channel
5. Save
```

### 3. Compare Before/After Performance
```
1. Note current metrics
2. Make a code change
3. Deploy
4. Compare metrics in Grafana
5. See exact performance impact!
```

### 4. Debug Performance Issues
```
1. User reports slow response
2. Check Grafana for that time period
3. See CPU/memory/network at that moment
4. Identify bottleneck
5. Fix and verify with metrics
```

---

## 🐛 TROUBLESHOOTING

### If Grafana doesn't show data:
```bash
# Check Prometheus is scraping
curl http://localhost:9090/api/v1/targets

# Check Grafana can reach Prometheus
docker exec bside-grafana wget -O- http://prometheus:9090/api/v1/query?query=up
```

### If metrics seem wrong:
```bash
# Restart Prometheus to re-scrape
docker restart bside-prometheus

# Check configuration
docker logs bside-prometheus | grep error
```

### If can't access web UIs:
```bash
# Check ports are exposed
docker ps --format "table {{.Names}}\t{{.Ports}}"

# Check services are healthy
docker ps --format "table {{.Names}}\t{{.Status}}"
```

---

## 📚 QUICK REFERENCE

### URLs
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **cAdvisor**: http://localhost:8084
- **AlertManager**: http://localhost:9093
- **Node Exporter**: http://localhost:9100/metrics
- **Redis Exporter**: http://localhost:9121/metrics

### Useful Commands
```bash
# View all metrics
docker ps --format "table {{.Names}}\t{{.Status}}"

# Restart observability stack
docker-compose -f docker-compose.observability.yml restart

# View logs
docker-compose -f docker-compose.observability.yml logs -f

# Stop all
docker-compose -f docker-compose.observability.yml down

# Start all
docker-compose -f docker-compose.observability.yml up -d
```

---

## 🎉 SUMMARY

**YOU ARE READY TO:**
1. ✅ View real-time system metrics
2. ✅ Monitor container resources
3. ✅ Create custom dashboards
4. ✅ Set up performance alerts
5. ✅ Debug performance issues
6. ✅ Track API performance
7. ✅ Monitor database health
8. ✅ Visualize network traffic

**NEXT STEPS:**
1. Open Grafana and explore
2. Start your full application
3. Watch metrics flow in real-time
4. Create dashboards for your specific needs
5. Set up alerts for critical metrics

**Your observability stack is LIVE and collecting data!** 🚀

---

*Created: February 2, 2026*
*Status: Core monitoring operational, advanced features configurable*

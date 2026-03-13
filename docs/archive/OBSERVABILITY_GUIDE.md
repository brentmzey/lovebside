# 🎯 Bside Observability Stack - Complete Guide

## 🚀 Quick Start - Accessing Your Monitoring Tools

### ✅ All Services Are Running!

Your complete observability stack is now operational with the following components:

---

## 📊 **Primary Dashboards**

### 1. **Grafana** - Main Visualization Dashboard
- **URL**: http://localhost:3000
- **Username**: `admin`
- **Password**: `admin123`
- **Purpose**: Unified dashboards for metrics, logs, and traces

**What to do first:**
1. Open http://localhost:3000 in your browser
2. Login with admin/admin123
3. Go to **Dashboards** → Browse pre-configured dashboards
4. Explore: System Metrics, Application Performance, Docker Stats

---

### 2. **Prometheus** - Metrics Database & Explorer
- **URL**: http://localhost:9090
- **Purpose**: Query and explore raw metrics data

**Useful Queries to Try:**
```promql
# CPU usage across all containers
rate(container_cpu_usage_seconds_total[5m])

# Memory usage
container_memory_usage_bytes

# HTTP request rate
rate(http_requests_total[1m])

# Database query latency
histogram_quantile(0.95, rate(db_query_duration_seconds_bucket[5m]))
```

---

### 3. **Jaeger** - Distributed Tracing
- **URL**: http://localhost:16686
- **Purpose**: Trace requests across your microservices

**How to Use:**
1. Select service (e.g., "bside-server")
2. Click "Find Traces"
3. Click on any trace to see the full request flow
4. Identify bottlenecks and slow operations

---

### 4. **Loki** - Log Aggregation
- **URL**: http://localhost:3100
- **Query via**: Grafana → Explore → Select Loki datasource

**Sample Queries:**
```logql
# All logs from backend service
{container="bside-server"}

# Error logs only
{container="bside-server"} |= "error" or "ERROR"

# Logs in the last 5 minutes with pattern
{container="bside-pocketbase"} |~ "database.*timeout"
```

---

### 5. **AlertManager** - Alert Management
- **URL**: http://localhost:9093
- **Purpose**: Manage and route alerts

---

## 🔍 **What to Monitor Right Now**

### System Health Checks

1. **Container Health**
```bash
docker compose -f docker-compose.observability.yml ps
```

2. **View Live Metrics** (Grafana):
   - Go to: http://localhost:3000
   - Navigate to: **Dashboards** → **Docker & System Metrics**
   - You'll see:
     - CPU usage per container
     - Memory consumption
     - Network I/O
     - Disk usage

3. **Application Performance** (Jaeger):
   - Go to: http://localhost:16686
   - Select your service
   - View traces to see:
     - Request latency
     - Database query times
     - External API calls
     - Error traces

---

## 📈 **Real-Time Monitoring Workflow**

### For Your Current Session:

#### Step 1: Check Overall System Health (Grafana)
```
1. Open: http://localhost:3000
2. Login: admin / admin123
3. Click: Dashboards → Browse
4. Select: "Docker Container & System Metrics"
```

**What You'll See:**
- ✅ All running containers
- 📊 CPU/Memory usage graphs
- 🌐 Network traffic
- 💾 Disk I/O

#### Step 2: Query Metrics (Prometheus)
```
1. Open: http://localhost:9090
2. Try this query in the search bar:
   rate(container_cpu_usage_seconds_total{name="bside-server"}[5m])
3. Click "Execute" and then "Graph"
```

#### Step 3: Trace Requests (Jaeger)
```
1. Open: http://localhost:16686
2. Select service from dropdown
3. Click "Find Traces"
4. Click any trace to see detailed breakdown
```

#### Step 4: View Logs (Grafana → Loki)
```
1. Open: http://localhost:3000
2. Click "Explore" (compass icon)
3. Select "Loki" from datasource dropdown
4. Use query: {container="bside-server"} |= "error"
```

---

## 🎪 **Testing Your Observability Stack**

### Generate Some Load to See Metrics

```bash
# Generate HTTP traffic to your backend
for i in {1..100}; do
  curl http://localhost:8081/api/health
  sleep 0.1
done

# Now check Grafana dashboards to see:
# - Request rate increase
# - Response time graphs
# - System resource usage
```

---

## 🔧 **Service Details & Ports**

| Service | URL | Port | Purpose |
|---------|-----|------|---------|
| **Grafana** | http://localhost:3000 | 3000 | Dashboards & Visualization |
| **Prometheus** | http://localhost:9090 | 9090 | Metrics Storage & Queries |
| **Jaeger UI** | http://localhost:16686 | 16686 | Trace Visualization |
| **Loki** | http://localhost:3100 | 3100 | Log Aggregation API |
| **AlertManager** | http://localhost:9093 | 9093 | Alert Management |
| **Tempo** | http://localhost:3200 | 3200 | Trace Storage Backend |
| **Node Exporter** | http://localhost:9100 | 9100 | Host Metrics |
| **cAdvisor** | http://localhost:8084 | 8084 | Container Metrics |
| **Redis Exporter** | http://localhost:9121 | 9121 | Redis Metrics |
| **OTEL Collector** | grpc://localhost:4317 | 4317 | Telemetry Ingestion |
| **OTEL Collector HTTP** | http://localhost:4318 | 4318 | Telemetry Ingestion |

---

## 📊 **Pre-configured Grafana Dashboards**

Your Grafana instance comes with these dashboards ready to use:

1. **Docker & System Metrics**
   - Container CPU/Memory usage
   - Network I/O
   - Disk usage
   - Container lifecycle events

2. **Application Performance**
   - Request rate & latency
   - Error rates
   - Database query performance
   - Cache hit rates

3. **Infrastructure Overview**
   - Node/host metrics
   - Redis performance
   - PocketBase metrics
   - Network topology

---

## 🚨 **Alert Rules Active**

Your Prometheus has these alerts configured:

- **High CPU Usage**: Container using >80% CPU for 5 minutes
- **High Memory Usage**: Container using >80% memory for 5 minutes
- **Service Down**: Any monitored service unreachable for 1 minute
- **High Error Rate**: Error rate >5% for 5 minutes
- **Slow Response Time**: P95 latency >1s for 5 minutes

Check alerts at: http://localhost:9093

---

## 🔍 **Troubleshooting**

### Check Service Logs
```bash
# View logs for any service
docker logs bside-grafana --tail 50
docker logs bside-prometheus --tail 50
docker logs bside-jaeger --tail 50
docker logs bside-otel-collector --tail 50
```

### Restart Specific Service
```bash
docker compose -f docker-compose.observability.yml restart grafana
```

### Full Stack Restart
```bash
cd /Users/brentzey/bside
./start-observability.sh development
```

### Stop Everything
```bash
docker compose -f docker-compose.observability.yml down
```

---

## 📚 **Learning Resources**

### Understanding Your Metrics

**CPU Usage**:
```promql
# Current CPU usage per container
rate(container_cpu_usage_seconds_total[1m]) * 100
```

**Memory Usage**:
```promql
# Memory usage in MB
container_memory_usage_bytes / 1024 / 1024
```

**Request Rate**:
```promql
# Requests per second
rate(http_requests_total[1m])
```

**Error Rate**:
```promql
# Percentage of failed requests
rate(http_requests_total{status=~"5.."}[5m]) / rate(http_requests_total[5m]) * 100
```

---

## 🎯 **Next Steps for Production**

1. **Instrument Your Application**:
   - Add OpenTelemetry SDK to your backend
   - Send traces to: `http://localhost:4318/v1/traces`
   - Send metrics to: `http://localhost:4318/v1/metrics`

2. **Create Custom Dashboards**:
   - Go to Grafana → Dashboards → New
   - Add panels with your business metrics

3. **Set Up Alerts**:
   - Edit: `observability/prometheus/alerts.yml`
   - Add your custom alert rules
   - Restart Prometheus

4. **Configure Notifications**:
   - Go to AlertManager: http://localhost:9093
   - Set up Slack/Email/PagerDuty integration

---

## 📝 **Quick Reference Commands**

```bash
# Start observability stack
./start-observability.sh development

# View all container status
docker compose -f docker-compose.observability.yml ps

# View container logs
docker logs -f bside-grafana

# Restart a service
docker compose -f docker-compose.observability.yml restart <service-name>

# Stop everything
docker compose -f docker-compose.observability.yml down

# Clean up volumes (careful!)
docker compose -f docker-compose.observability.yml down -v
```

---

## 🎉 **You're All Set!**

Your complete observability stack is running and ready to monitor your application!

**Start Here:**
1. Open Grafana: http://localhost:3000 (admin/admin123)
2. Explore pre-built dashboards
3. Check Jaeger for traces: http://localhost:16686
4. Query Prometheus: http://localhost:9090

**Need Help?**
- Check the logs: `docker logs bside-grafana`
- Verify services: `docker compose -f docker-compose.observability.yml ps`
- Restart if needed: `./start-observability.sh development`

---

**Last Updated**: 2026-02-01
**Stack Version**: Production-Ready v1.0
**Status**: ✅ All Systems Operational

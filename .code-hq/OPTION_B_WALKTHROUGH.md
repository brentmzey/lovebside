# 🎯 Option B: Enhance Local Stack - Live Walkthrough

**Date:** 2026-01-31 10:23 UTC  
**Duration:** 30-45 minutes  
**Goal:** See everything working, create your first dashboards!

---

## 🌐 Open These URLs Now:

1. **Grafana:** http://localhost:3000 (admin/admin)
2. **Prometheus:** http://localhost:9090
3. **Main App:** http://localhost:8082
4. **Backend API:** http://localhost:8081/health
5. **PocketBase:** http://localhost:8092
6. **GoAccess:** http://localhost:7817
7. **Redis UI:** http://localhost:8083

---

## Part 1: Configure Grafana (10 min)

### Step 1: Login to Grafana
1. Open http://localhost:3000
2. Login: `admin` / `admin`
3. You'll be prompted to change password (skip for now)

### Step 2: Add Prometheus Datasource
1. Click **⚙️ Settings** (gear icon) → **Data Sources**
2. Click **Add data source**
3. Select **Prometheus**
4. Configure:
   ```
   Name: Prometheus
   URL: http://prometheus:9090
   ```
5. Scroll down, click **Save & Test**
6. Should see: ✅ "Data source is working"

### Step 3: Add Loki Datasource
1. Click **Add data source** again
2. Select **Loki**
3. Configure:
   ```
   Name: Loki
   URL: http://loki:3100
   ```
4. Click **Save & Test**
5. Should see: ✅ "Data source connected and labels found"

---

## Part 2: Import Pre-Built Dashboards (5 min)

### Dashboard 1: Node Exporter Full (System Metrics)

1. Click **+** icon → **Import**
2. Enter dashboard ID: `1860`
3. Click **Load**
4. Select **Prometheus** as datasource
5. Click **Import**

**What you'll see:**
- CPU usage (all cores)
- Memory usage
- Disk I/O
- Network traffic
- System load
- Uptime

### Dashboard 2: Redis Dashboard

1. Click **+** → **Import**
2. Enter ID: `763`
3. Load → Select **Prometheus** → Import

**What you'll see:**
- Commands per second
- Hit rate
- Memory usage
- Connected clients
- Key statistics

### Dashboard 3: Container Metrics (cAdvisor)

1. Click **+** → **Import**
2. Enter ID: `14282`
3. Load → Select **Prometheus** → Import

**What you'll see:**
- CPU per container
- Memory per container
- Network I/O per container
- Disk I/O per container

---

## Part 3: Explore Live Data (10 min)

### 3.1 Query Prometheus Directly

Open http://localhost:9090

**Try these queries:**

```promql
# System CPU usage
100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Memory usage percentage
100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))

# Redis commands per second
rate(redis_commands_processed_total[1m])

# Redis hit rate
redis_keyspace_hits / (redis_keyspace_hits + redis_keyspace_misses)

# Container CPU usage
rate(container_cpu_usage_seconds_total{name=~"bside-.*"}[1m])

# Container memory
container_memory_usage_bytes{name=~"bside-.*"}
```

**How to use:**
1. Paste query in "Expression" box
2. Click **Execute**
3. View as **Graph** or **Table**
4. Adjust time range (top right)

### 3.2 Explore Logs in Grafana

1. Go back to Grafana: http://localhost:3000
2. Click **Explore** (compass icon)
3. Select **Loki** datasource (top left dropdown)
4. Try these LogQL queries:

```logql
# All logs from containers
{job="varlogs"}

# Backend logs (if available)
{service="backend"}

# Error logs only
{job="varlogs"} |= "error"

# Last 5 minutes, case insensitive
{job="varlogs"} [5m] |= "(?i)error"
```

**Live tail:**
- Toggle "Live" in top right
- Watch logs stream in real-time

### 3.3 Check GoAccess (Real-time Web Stats)

Open http://localhost:7817

**You'll see:**
- Requests per second (live)
- Response time distribution
- Top URLs
- HTTP status codes
- User agents
- Bandwidth usage

**This updates in REAL-TIME!**

### 3.4 Redis Commander (Browse Cache)

Open http://localhost:8083

**Features:**
- Browse all keys
- View key contents
- TTL information
- Memory usage
- Execute commands

---

## Part 4: Create Your First Custom Dashboard (10 min)

### Create "Bside Application Overview"

1. In Grafana, click **+** → **Dashboard**
2. Click **Add visualization**
3. Select **Prometheus** datasource

### Panel 1: System Health

**Query:**
```promql
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

**Settings:**
- Title: "System CPU Usage"
- Unit: "Percent (0-100)"
- Thresholds: 70 (yellow), 85 (red)

Click **Apply**

### Panel 2: Memory Usage

Click **Add panel**

**Query:**
```promql
100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))
```

**Settings:**
- Title: "Memory Usage"
- Unit: "Percent (0-100)"
- Visualization: Gauge

Click **Apply**

### Panel 3: Redis Performance

Add another panel

**Query:**
```promql
rate(redis_commands_processed_total[1m])
```

**Settings:**
- Title: "Redis Commands/sec"
- Visualization: Time series

Click **Apply**

### Panel 4: Container Memory

**Query:**
```promql
container_memory_usage_bytes{name=~"bside-.*"} / 1024 / 1024
```

**Settings:**
- Title: "Container Memory (MB)"
- Legend: "{{name}}"
- Visualization: Time series

Click **Apply**

### Save Dashboard

1. Click **💾 Save dashboard** (top right)
2. Name: "Bside Application Overview"
3. Click **Save**

---

## Part 5: Set Up Alerts (5 min)

### Create Your First Alert Rule

1. Go to **Alerting** → **Alert rules**
2. Click **New alert rule**

**Alert: High CPU Usage**

```yaml
Name: High CPU Usage
Query: 100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
Condition: IS ABOVE 80
For: 5 minutes
```

**Annotations:**
- Summary: System CPU usage is high
- Description: CPU usage is {{ $value }}%

Click **Save**

### Create More Alerts (Copy/Paste)

**High Memory:**
```
Query: 100 * (1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes))
Condition: IS ABOVE 85
For: 5 minutes
```

**Redis Down:**
```
Query: up{job="redis"}
Condition: IS BELOW 1
For: 1 minute
```

**Backend Down:**
```
Query: up{job="backend"}
Condition: IS BELOW 1
For: 1 minute
```

---

## Part 6: Generate Some Load & Watch Metrics (5 min)

Let's create some activity to see metrics change!

### Terminal 1: Generate API Traffic

```bash
# Simple load test
for i in {1..1000}; do
  curl -s http://localhost:8081/health > /dev/null
  sleep 0.1
done
```

### Terminal 2: Generate Redis Traffic

```bash
# Redis commands
for i in {1..100}; do
  docker exec bside-redis redis-cli SET "test:$i" "value$i" EX 60
  docker exec bside-redis redis-cli GET "test:$i"
  sleep 0.1
done
```

### Watch the Dashboards!

1. Go to your "Bside Application Overview" dashboard
2. Set refresh to **5s** (top right dropdown)
3. Watch metrics update in real-time!

You should see:
- CPU usage spike slightly
- Redis commands/sec increase
- Container metrics change
- Network traffic in GoAccess

---

## Part 7: Verify Everything Works (5 min)

### Quick Health Check Script

```bash
#!/bin/bash
echo "=== Full Stack Health Check ==="
echo ""

echo "Core Services:"
curl -s http://localhost:8082/health && echo " ✅ Nginx"
curl -s http://localhost:8081/health | jq -r '.status' && echo " ✅ Backend"
curl -s http://localhost:8092/api/health | jq -r '.message' && echo " ✅ PocketBase"
redis-cli -h localhost -p 6379 PING && echo " ✅ Redis"

echo ""
echo "Monitoring:"
curl -s http://localhost:9090/-/healthy && echo " ✅ Prometheus"
curl -s http://localhost:3100/ready | head -1 && echo " ✅ Loki"
curl -s http://localhost:3000/api/health | jq -r '.database' && echo " ✅ Grafana"

echo ""
echo "Metrics:"
curl -s http://localhost:9100/metrics | grep -c "node_" && echo " metrics from Node Exporter"
curl -s http://localhost:8080/metrics | grep -c "container_" && echo " metrics from cAdvisor"
curl -s http://localhost:9121/metrics | grep -c "redis_" && echo " metrics from Redis Exporter"

echo ""
echo "🎉 All services operational!"
```

Save as `check-stack.sh`, make executable, run it:

```bash
chmod +x check-stack.sh
./check-stack.sh
```

---

## 🎊 Success Checklist

After completing this walkthrough, you should have:

- [x] Logged into Grafana
- [x] Added Prometheus and Loki datasources
- [x] Imported 3 pre-built dashboards (Node, Redis, Containers)
- [x] Queried metrics in Prometheus
- [x] Explored logs in Loki
- [x] Created your first custom dashboard
- [x] Set up at least 2 alert rules
- [x] Generated load and watched metrics change
- [x] Verified all 12 services are healthy

---

## 💡 Pro Tips

### Grafana Shortcuts
- `Ctrl+K` - Open command palette
- `d+d` - Go to dashboard
- `d+e` - Explore
- `d+h` - Home

### Useful PromQL Functions
```promql
# Average over time
avg_over_time(metric[5m])

# Rate of increase
rate(metric[5m])

# Top 10
topk(10, metric)

# Aggregations
sum by (label) (metric)
avg by (label) (metric)
max by (label) (metric)
```

### Grafana Variables (Advanced)
Create dashboard variables for:
- Time ranges
- Environments (dev/staging/prod)
- Instances
- Services

---

## 🐛 Troubleshooting

### "Data source is not working"
```bash
# Check Prometheus is running
docker ps | grep prometheus

# Check Prometheus is accessible
curl http://localhost:9090/-/healthy

# Check from within Grafana container
docker exec -it bside-grafana curl http://prometheus:9090/-/healthy
```

### No metrics showing
```bash
# Check Prometheus targets
# Go to: http://localhost:9090/targets
# All should be "UP"

# Check Prometheus config
docker exec bside-prometheus cat /etc/prometheus/prometheus.yml
```

### Logs not appearing in Loki
```bash
# Check Loki
curl http://localhost:3100/ready

# Check if Loki has data
curl http://localhost:3100/loki/api/v1/labels

# Note: Promtail has permission issues on macOS, so log shipping is limited
# Loki still works, just might not have all logs
```

---

## 📸 Screenshots to Take

For your documentation/demo:

1. Grafana home with all dashboards
2. Node Exporter dashboard showing system metrics
3. Redis dashboard showing cache performance
4. Your custom "Bside Application Overview" dashboard
5. Prometheus query showing container metrics
6. Loki log explorer with logs streaming
7. GoAccess real-time web stats
8. Alerting rules page

---

## Next: Test the Stack

Once you've configured everything:

1. **Load Test**
   ```bash
   # Install k6
   brew install k6
   
   # Run load test
   k6 run - <<EOF
   import http from 'k6/http';
   export const options = { vus: 50, duration: '2m' };
   export default function() {
     http.get('http://localhost:8082/');
   }
   EOF
   ```

2. **Watch Metrics Change**
   - Open your dashboard
   - Set auto-refresh to 5s
   - Run load test
   - Watch CPU, memory, requests spike!

3. **Trigger an Alert**
   ```bash
   # Eat up CPU (careful!)
   stress --cpu 8 --timeout 60s
   
   # Or in Docker
   docker run --rm --name cpu-stress alexeiled/stress-ng --cpu 4 --timeout 60s
   ```

4. **Check Alert Fired**
   - Go to Alerting → Alert rules
   - Should see "High CPU Usage" firing
   - Check firing time

---

## 🎯 What You've Accomplished

You now have:
- ✅ Full observability of your application
- ✅ Real-time metrics and dashboards
- ✅ Log aggregation and search
- ✅ Alerting on key metrics
- ✅ The same monitoring Fortune 500 companies use!

This is **production-grade monitoring** that you can now:
- Take to staging (when ready)
- Take to production
- Use for debugging issues
- Use for capacity planning
- Use for performance optimization

---

**Time spent:** ~45 minutes  
**Value gained:** Priceless! 💎

Ready for **Option C: Plan Next Phase**?

# 🚀 Complete Local Stack Walkthrough
**Your Step-by-Step Guide to Seeing Everything Work**

**Date:** 2026-01-31  
**Time Required:** 30 minutes  
**Prerequisites:** Docker Desktop running

---

## 📋 Table of Contents

1. [Starting the Stack](#1-starting-the-stack)
2. [Verifying Services](#2-verifying-services)
3. [Opening Grafana](#3-opening-grafana)
4. [Importing Dashboards](#4-importing-dashboards)
5. [Viewing Metrics](#5-viewing-metrics)
6. [Checking Logs](#6-checking-logs)
7. [Testing the Backend](#7-testing-the-backend)
8. [Inspecting the Database](#8-inspecting-the-database)
9. [Viewing Redis](#9-viewing-redis)
10. [Next Steps](#10-next-steps)

---

## 1. Starting the Stack

### Current Status Check

First, let's see if everything is already running:

\`\`\`bash
cd ~/bside
docker-compose -f docker-compose.enhanced-lite.yml ps
\`\`\`

**Expected:** You should see 12 services listed.

### If Not Running, Start It

\`\`\`bash
cd ~/bside
./start-full-stack.sh

# OR manually:
docker-compose -f docker-compose.enhanced-lite.yml up -d
\`\`\`

**Wait 30 seconds** for all services to start.

### Verify All Services Started

\`\`\`bash
./TEST-EVERYTHING.sh
\`\`\`

**Expected Output:**
- ✅ 13/15 tests passing
- 2 minor failures (Nginx 404, GoAccess) - these are OK for now

---

## 2. Verifying Services

### Quick Health Check

Run this single command to check everything:

\`\`\`bash
cd ~/bside
echo "=== Service Health ===" && \
curl -s http://localhost:8081/health && echo " ✅ Backend" && \
curl -s http://localhost:8092/api/health && echo " ✅ Database" && \
curl -s http://localhost:9090/-/healthy && echo " ✅ Prometheus" && \
curl -s http://localhost:3000/api/health && echo " ✅ Grafana"
\`\`\`

**Expected:** All should return successful responses.

### Full Service List

| Service | URL | Purpose |
|---------|-----|---------|
| **Nginx** | http://localhost:8082 | Reverse proxy |
| **Backend API** | http://localhost:8081 | Ktor server |
| **PocketBase** | http://localhost:8092 | Database & API |
| **Redis UI** | http://localhost:8083 | Cache inspector |
| **Prometheus** | http://localhost:9090 | Metrics |
| **Grafana** | http://localhost:3000 | Dashboards |
| **Loki** | http://localhost:3100 | Logs |
| **Node Exporter** | http://localhost:9100/metrics | System metrics |
| **cAdvisor** | http://localhost:8080 | Container metrics |

---

## 3. Opening Grafana

### Step 1: Open in Browser

\`\`\`bash
open http://localhost:3000
\`\`\`

Or manually open: **http://localhost:3000**

### Step 2: Login

**Username:** admin  
**Password:** admin

**IMPORTANT:** Grafana will ask you to change the password on first login.
- You can change it or click "Skip"

### Step 3: Verify You're In

You should see the Grafana home page with:
- Left sidebar with menu
- "Welcome to Grafana" message
- Options to add datasources or create dashboards

---

## 4. Importing Dashboards

### Why Import Dashboards?

Dashboards visualize your metrics. Instead of building from scratch, we'll import pre-built professional dashboards.

### Step-by-Step: Import Dashboard 1860 (System Metrics)

#### Step 1: Click "+" Button

In the left sidebar, find the **"+"** icon (looks like a plus sign).

Click it → Select **"Import dashboard"**

#### Step 2: Enter Dashboard ID

You'll see a form with "Import via grafana.com"

**Enter:** `1860`

**Press Enter** or click **"Load"**

#### Step 3: Configure Import

You'll see a preview of "Node Exporter Full"

**Important Settings:**
- **Name:** Node Exporter Full (default is fine)
- **Folder:** General (default is fine)  
- **Prometheus:** Select "Prometheus" from dropdown

**Click:** "Import" button

#### Step 4: Success!

You should now see a beautiful dashboard with:
- CPU usage graphs
- Memory usage
- Disk I/O
- Network traffic
- Load average

**This is LIVE data from your system!**

### Repeat for Other Dashboards

**Dashboard 763 - Redis:**

1. Click "+" → "Import dashboard"
2. Enter: `763`
3. Click "Load"
4. Select "Prometheus" datasource
5. Click "Import"

**Result:** Redis cache metrics dashboard

**Dashboard 14282 - Docker Containers:**

1. Click "+" → "Import dashboard"  
2. Enter: `14282`
3. Click "Load"
4. Select "Prometheus" datasource
5. Click "Import"

**Result:** All container resource usage

### Troubleshooting Dashboard Import

**Problem:** "Dashboard not found"
- **Solution:** Make sure you're connected to the internet
- Grafana downloads dashboards from grafana.com

**Problem:** "No data"
- **Solution:** Wait 30 seconds for Prometheus to collect metrics
- Check datasource is selected correctly

**Problem:** Datasource dropdown is empty
- **Solution:** Run `./setup-grafana.sh` to auto-configure datasources

---

## 5. Viewing Metrics in Prometheus

### Open Prometheus

\`\`\`bash
open http://localhost:9090
\`\`\`

Or manually: **http://localhost:9090**

### Try Some Queries

In the **Expression** box at the top, try these:

**Query 1: System Memory Available**
\`\`\`promql
node_memory_MemAvailable_bytes / 1024 / 1024 / 1024
\`\`\`
Click **"Execute"** → See available memory in GB

**Query 2: Request Rate (if backend has traffic)**
\`\`\`promql
rate(http_requests_total[5m])
\`\`\`

**Query 3: Redis Memory Usage**
\`\`\`promql
redis_memory_used_bytes / 1024 / 1024
\`\`\`
Shows Redis memory in MB

**Query 4: Container CPU Usage**
\`\`\`promql
sum(rate(container_cpu_usage_seconds_total[5m])) by (name)
\`\`\`

### Exploring Metrics

**Click "Globe" icon** → Select "Metrics Explorer"

This shows ALL available metrics (you have 815+!)

Browse through to see what's being collected.

---

## 6. Checking Logs

### Method 1: Loki via Grafana

1. Open Grafana: http://localhost:3000
2. Click **"Explore"** icon (compass) in left sidebar
3. Top dropdown: Select **"Loki"**
4. Try query: `{container_name="bside-server"}`
5. Click **"Run query"**

**Result:** See all logs from backend server

**Try filtering for errors:**
\`\`\`logql
{container_name="bside-server"} |= "ERROR"
\`\`\`

### Method 2: Docker Logs

**View all services:**
\`\`\`bash
cd ~/bside
docker-compose -f docker-compose.enhanced-lite.yml logs -f
\`\`\`
Press **Ctrl+C** to stop

**View specific service:**
\`\`\`bash
docker-compose -f docker-compose.enhanced-lite.yml logs -f server
\`\`\`

**Last 50 lines only:**
\`\`\`bash
docker-compose -f docker-compose.enhanced-lite.yml logs --tail=50 server
\`\`\`

---

## 7. Testing the Backend

### Health Check

\`\`\`bash
curl http://localhost:8081/health
\`\`\`

**Expected:** JSON response with status

### Via Browser

Open: **http://localhost:8081/health**

### Check What Endpoints Exist

\`\`\`bash
curl http://localhost:8081/api/
\`\`\`

**Note:** The backend is a Ktor server. Check `server/src/main/kotlin/` for routes.

---

## 8. Inspecting the Database (PocketBase)

### Open PocketBase Admin UI

\`\`\`bash
open http://localhost:8092/_/
\`\`\`

Or manually: **http://localhost:8092/_/**

### Login

If this is first time, you'll need to create admin account:
- **Email:** admin@example.com
- **Password:** (your choice)

### What You Can Do

- **Collections:** View database schema
- **Records:** Browse/edit data
- **Logs:** See API requests
- **Settings:** Configure database

### API Access

**Health Check:**
\`\`\`bash
curl http://localhost:8092/api/health
\`\`\`

**List Collections:**
\`\`\`bash
curl http://localhost:8092/api/collections
\`\`\`

---

## 9. Viewing Redis

### Open Redis Commander UI

\`\`\`bash
open http://localhost:8083
\`\`\`

Or manually: **http://localhost:8083**

### What You Can See

- **Keys:** All keys stored in Redis
- **Values:** Inspect key contents
- **Stats:** Memory usage, hit rate
- **CLI:** Run Redis commands

### Via Command Line

**Access Redis CLI:**
\`\`\`bash
docker exec -it bside-redis redis-cli
\`\`\`

**Inside Redis CLI, try:**
\`\`\`redis
PING
# Should return: PONG

INFO memory
# Shows memory usage

DBSIZE
# Shows number of keys

KEYS *
# Lists all keys (use cautiously in production!)

exit
# Exit CLI
\`\`\`

---

## 10. Next Steps

### Immediate (You've Done These!)

- ✅ Stack is running
- ✅ Grafana opened
- ✅ Dashboards imported (3)
- ✅ Metrics viewed in Prometheus
- ✅ Logs checked

### Today (Next 1-2 hours)

#### A. Fix Nginx Health Check

**Problem:** Nginx returns 404 because no frontend is deployed

**Solution Option 1 - Quick Fix:**
\`\`\`bash
cd ~/bside/nginx
cat >> nginx.conf << 'NGINX_EOF'

    location / {
        return 200 'Bside Stack Running';
        add_header Content-Type text/plain;
    }
NGINX_EOF

docker-compose -f docker-compose.enhanced-lite.yml restart nginx
\`\`\`

**Solution Option 2 - Deploy Frontend:**
\`\`\`bash
cd ~/bside/composeApp
./gradlew wasmJsBrowserDevelopmentExecutableDistribution
cp -r build/dist/wasmJs/productionExecutable/* ../nginx/html/
docker-compose -f docker-compose.enhanced-lite.yml restart nginx
\`\`\`

**Test:**
\`\`\`bash
curl http://localhost:8082
# Should now return 200
\`\`\`

#### B. Create a Custom Dashboard

**Follow:** `.code-hq/OPTION_B_WALKTHROUGH.md` (section on custom dashboards)

**Quick Version:**

1. In Grafana, click "+" → "Dashboard"
2. Click "Add visualization"
3. Select "Prometheus" datasource
4. Enter query (e.g., `rate(http_requests_total[5m])`)
5. Choose visualization type (Graph, Gauge, etc.)
6. Click "Apply"
7. Click "Save dashboard" (top right)
8. Name it "Backend API Performance"

#### C. Generate Some Traffic

**Create a simple load test:**

\`\`\`bash
# Make 100 requests to backend
for i in {1..100}; do
  curl -s http://localhost:8081/health > /dev/null
  echo "Request $i"
  sleep 0.1
done
\`\`\`

**Watch metrics update in Grafana dashboards!**

### This Week

1. **Install k6 for load testing:**
   \`\`\`bash
   brew install k6
   \`\`\`

2. **Create load test script:**
   \`\`\`bash
   mkdir -p ~/bside/tests/load
   cat > ~/bside/tests/load/baseline.js << 'K6_EOF'
import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 10 },  // Ramp up to 10 users
    { duration: '1m', target: 10 },   // Stay at 10 users
    { duration: '30s', target: 0 },   // Ramp down
  ],
};

export default function () {
  let res = http.get('http://localhost:8081/health');
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(1);
}
K6_EOF
   \`\`\`

3. **Run load test:**
   \`\`\`bash
   k6 run ~/bside/tests/load/baseline.js
   \`\`\`

4. **Watch results in Grafana** while test is running!

### Resources

**Documentation:**
- Full stack guide: `.code-hq/FULL_STACK_GUIDE.md`
- Quick reference: `.code-hq/QUICK_REFERENCE.md`
- Troubleshooting: See any guide, search for error messages

**Common Commands:**
\`\`\`bash
# View all services
docker-compose -f docker-compose.enhanced-lite.yml ps

# Restart a service
docker-compose -f docker-compose.enhanced-lite.yml restart [service]

# Stop everything
docker-compose -f docker-compose.enhanced-lite.yml down

# Start everything
docker-compose -f docker-compose.enhanced-lite.yml up -d

# View logs
docker-compose -f docker-compose.enhanced-lite.yml logs -f [service]
\`\`\`

---

## 🎉 Congratulations!

You've now:
- ✅ Seen the full stack running locally
- ✅ Opened and configured Grafana
- ✅ Imported 3 professional dashboards
- ✅ Viewed live metrics in Prometheus
- ✅ Checked logs in multiple ways
- ✅ Tested backend and database
- ✅ Inspected Redis cache
- ✅ Know how to manage the stack

**You have a production-grade observability stack running on your laptop!**

---

**Questions?** Check `.code-hq/INDEX.md` for all documentation.

**Next?** Follow the "This Week" section above to continue.

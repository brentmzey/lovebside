# 🚀 Complete System Startup Guide

## Quick Start - Full Stack with Observability

### Step 1: Fix and Start Observability Stack

```bash
# Fix the startup script issue
./start-observability.sh .env.observability.development

# Or use the corrected command
ENV=development docker-compose -f docker-compose.observability.yml up -d
```

### Step 2: Start Main Application Stack

```bash
# Start PocketBase, Redis, and backend services
docker-compose up -d

# Or use the full stack
./start-full-stack.sh
```

### Step 3: Access Dashboards

Once everything is running, access these URLs:

#### 📊 Observability Dashboards

1. **Grafana** - Main Dashboard
   - URL: http://localhost:3000
   - User: `admin`
   - Password: `admin123`
   - Dashboards: Metrics, Logs, Traces

2. **Prometheus** - Metrics
   - URL: http://localhost:9090
   - Query metrics directly
   - View targets and alerts

3. **Jaeger** - Distributed Tracing
   - URL: http://localhost:16686
   - View request traces
   - Analyze performance

4. **AlertManager** - Alerts
   - URL: http://localhost:9093
   - View active alerts
   - Manage notifications

#### 🎯 Application Services

5. **PocketBase Admin** 
   - URL: http://localhost:8090/_/
   - Manage database
   - View collections

6. **Backend API**
   - URL: http://localhost:8080
   - Health: http://localhost:8080/health
   - Metrics: http://localhost:8080/metrics

### Step 4: View Real-Time Metrics

#### In Grafana:
1. Go to http://localhost:3000
2. Navigate to "Dashboards"
3. Open "Bside Application Overview"
4. See real-time:
   - Request rates
   - Error rates
   - Response times
   - System resources

#### In Prometheus:
1. Go to http://localhost:9090
2. Try these queries:
   ```promql
   # Request rate
   rate(http_requests_total[5m])
   
   # Error rate
   rate(http_requests_total{status=~"5.."}[5m])
   
   # P95 latency
   histogram_quantile(0.95, http_request_duration_seconds_bucket)
   ```

#### In Jaeger:
1. Go to http://localhost:16686
2. Select "bside-backend" service
3. Click "Find Traces"
4. Click on any trace to see:
   - Request path
   - Database queries
   - External API calls
   - Timing breakdown

### Step 5: Test the System

```bash
# Run health checks
curl http://localhost:8080/health

# Send test requests
for i in {1..100}; do
  curl http://localhost:8080/api/messages &
done

# Watch metrics in Grafana in real-time!
```

---

## 🔧 Troubleshooting

### Observability Stack Won't Start

**Error**: `Environment file not found: development`

**Solution**:
```bash
# Use the full env file name
./start-observability.sh .env.observability.development

# Or set environment variable
ENV=development docker-compose -f docker-compose.observability.yml up -d
```

### Port Conflicts

If ports are already in use:

```bash
# Check what's using ports
lsof -i :3000  # Grafana
lsof -i :9090  # Prometheus
lsof -i :16686 # Jaeger

# Kill processes if needed
kill -9 <PID>
```

### Services Not Showing Metrics

1. Check if services are running:
   ```bash
   docker-compose ps
   ```

2. Check service logs:
   ```bash
   docker-compose logs -f prometheus
   docker-compose logs -f grafana
   ```

3. Verify Prometheus targets:
   - Go to http://localhost:9090/targets
   - All should be "UP"

---

## 🎯 AWS CDN Implementation

### Next Steps for Media Storage

1. **Review the implementation guide**:
   ```bash
   cat AWS_CDN_IMPLEMENTATION_GUIDE.md
   ```

2. **Set up AWS credentials**:
   ```bash
   # Add to .env
   AWS_REGION=us-east-1
   AWS_ACCESS_KEY_ID=your_key
   AWS_SECRET_ACCESS_KEY=your_secret
   AWS_S3_BUCKET=bside-media-dev
   AWS_CLOUDFRONT_DOMAIN=your_distribution.cloudfront.net
   ```

3. **Create AWS resources**:
   - S3 bucket for media storage
   - CloudFront distribution for CDN
   - IAM user with appropriate permissions

4. **Enable CDN in config**:
   ```bash
   # In .env
   ENABLE_CDN=true
   ENABLE_S3_UPLOAD=true
   ```

---

## 📊 Performance Monitoring While Running

### Real-Time Dashboard Views

1. **System Overview**
   - CPU usage across services
   - Memory consumption
   - Disk I/O
   - Network traffic

2. **Application Metrics**
   - Requests per second
   - Response times (p50, p95, p99)
   - Error rates
   - Active connections

3. **Database Performance**
   - Query execution times
   - Connection pool usage
   - Cache hit rates
   - Slow queries

4. **Message Queue**
   - Messages in queue
   - Processing rate
   - Failed messages
   - Consumer lag

### Setting Up Alerts

Grafana automatically includes alerts for:
- High error rate (> 5%)
- High latency (p95 > 1s)
- Low CPU/Memory (< 10% free)
- Service down

Edit alerts in Grafana → Alerting → Alert Rules

---

## 🎮 Load Testing

### Generate Test Load

```bash
# Install Apache Bench (if not installed)
brew install ab  # macOS
apt install apache2-utils  # Linux

# Run load test
ab -n 10000 -c 100 http://localhost:8080/api/messages

# Watch metrics in Grafana while test runs!
```

### Use k6 for Advanced Testing

```javascript
// load-test.js
import http from 'k6/http';

export default function() {
  http.get('http://localhost:8080/api/messages');
}

export let options = {
  vus: 100,
  duration: '5m',
};
```

```bash
# Run k6 test
k6 run load-test.js

# View results in Grafana
```

---

## 📝 Quick Reference Commands

```bash
# Start everything
docker-compose up -d
ENV=development docker-compose -f docker-compose.observability.yml up -d

# Stop everything
docker-compose down
docker-compose -f docker-compose.observability.yml down

# View logs
docker-compose logs -f [service_name]

# Restart a service
docker-compose restart [service_name]

# Check service health
curl http://localhost:8080/health
curl http://localhost:9090/-/healthy
curl http://localhost:3000/api/health

# View metrics
curl http://localhost:8080/metrics
curl http://localhost:9090/api/v1/query?query=up
```

---

## ✅ System Status Checklist

Run these checks to verify everything is working:

### Observability Stack
- [ ] Grafana accessible at :3000
- [ ] Prometheus accessible at :9090
- [ ] Jaeger accessible at :16686
- [ ] All Prometheus targets UP
- [ ] Grafana dashboards loading
- [ ] Traces appearing in Jaeger

### Application Stack
- [ ] PocketBase accessible at :8090
- [ ] Backend API accessible at :8080
- [ ] Redis responding
- [ ] Health checks passing
- [ ] Metrics being collected

### Performance
- [ ] Response time < 100ms (p95)
- [ ] Error rate < 1%
- [ ] CPU usage < 70%
- [ ] Memory usage < 80%

---

## 🎓 Learning Resources

- Grafana Docs: https://grafana.com/docs/
- Prometheus Query Language: https://prometheus.io/docs/prometheus/latest/querying/basics/
- Jaeger Tracing: https://www.jaegertracing.io/docs/
- OpenTelemetry: https://opentelemetry.io/docs/

---

**Last Updated**: February 1, 2026
**Status**: ✅ Ready to use
**Support**: Check logs and dashboards for issues


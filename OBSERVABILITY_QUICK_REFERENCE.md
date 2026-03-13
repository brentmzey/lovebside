# Observability Stack - Quick Reference Card

## 🚀 Quick Commands

```bash
# Check status
./observability-status.sh

# Start (development)
./start-observability.sh development

# Start (production)
./start-observability.sh production

# Stop
docker-compose -f docker-compose.observability.yml down

# Restart service
docker-compose -f docker-compose.observability.yml restart <service>

# View logs
docker-compose -f docker-compose.observability.yml logs -f <service>

# Resource usage
docker stats
```

## 📊 Access URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | admin/admin |
| **Prometheus** | http://localhost:9090 | - |
| **Jaeger UI** | http://localhost:16686 | - |
| **Node Exporter** | http://localhost:9100 | - |
| **cAdvisor** | http://localhost:8084 | - |
| **Backend API** | http://localhost:8081 | - |
| **PocketBase** | http://localhost:8092 | - |
| **Redis UI** | http://localhost:8083 | - |
| **Kibana** | http://localhost:5601 | - |

## 🔍 Common Prometheus Queries

```promql
# CPU Usage
100 - (avg(irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Memory Usage
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100

# HTTP Request Rate
rate(http_server_requests_seconds_count[5m])

# HTTP Error Rate  
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# Container CPU
rate(container_cpu_usage_seconds_total{name="bside-server"}[5m])

# Container Memory
container_memory_usage_bytes{name="bside-server"}

# Database Connections
hikaricp_connections_active
```

## 📈 Grafana Dashboards to Import

| ID | Name | Purpose |
|----|------|---------|
| 1860 | Node Exporter Full | System metrics |
| 893 | Docker Monitoring | Container metrics |
| 14282 | Spring Boot 3.x | Application metrics |
| 11835 | Redis | Redis metrics |
| 763 | Jaeger | Trace analytics |

## 🎯 Jaeger Endpoints

```bash
# UI
http://localhost:16686

# HTTP Collector
http://localhost:14268

# gRPC Collector
localhost:14250

# Zipkin Compatible
http://localhost:9411
```

## 🔧 Environment Files

- Development: `.env.observability.development`
- Production: `.env.observability.production`

## 📝 Log Queries (Loki/LogQL)

```logql
# All logs from backend
{container="bside-server"}

# Error logs only
{container="bside-server"} |= "ERROR"

# JSON field filter
{container="bside-server"} | json | level="error"

# Error rate
rate({container="bside-server"} |= "ERROR"[5m])
```

## 🚨 Alert Examples

```yaml
# High CPU
avg(rate(node_cpu_seconds_total{mode!="idle"}[5m])) > 0.8

# High Memory
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes > 0.9

# Container Down
up{job="docker"} == 0

# High Error Rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
```

## 🛠️ Troubleshooting

```bash
# Service not responding
docker-compose -f docker-compose.observability.yml restart <service>

# Check logs
docker-compose -f docker-compose.observability.yml logs <service>

# Verify metrics endpoint
curl http://localhost:8081/actuator/prometheus

# Test Prometheus scraping
docker exec bside-prometheus wget -O- http://server:8080/actuator/prometheus

# Check Jaeger health
curl http://localhost:14269/

# View container resources
docker stats --no-stream

# Cleanup
docker-compose -f docker-compose.observability.yml down -v
docker system prune -a
```

## 📦 Service Dependencies

```
Application → OpenTelemetry Collector → Jaeger/Prometheus
           ↓
         Metrics Endpoint → Prometheus → Grafana
           ↓
         Logs → Loki → Grafana
```

## 🔐 Production Checklist

- [ ] Change Grafana password
- [ ] Enable authentication on Prometheus
- [ ] Configure AlertManager notifications
- [ ] Set up HTTPS/TLS
- [ ] Configure backup retention
- [ ] Set resource limits
- [ ] Enable log aggregation
- [ ] Configure trace sampling (10-20%)
- [ ] Set up monitoring alerts
- [ ] Document SLOs/SLIs

## 📚 Key Concepts

### Metrics (RED Method)
- **R**ate: Requests per second
- **E**rrors: Error rate
- **D**uration: Response time

### Metrics (USE Method)
- **U**tilization: % resource used
- **S**aturation: Queue length
- **E**rrors: Error count

### The Four Golden Signals
1. **Latency**: Request duration
2. **Traffic**: Request rate
3. **Errors**: Error rate
4. **Saturation**: Resource usage

## 🎯 SLI/SLO Examples

```yaml
# Availability SLO
SLI: Percentage of successful requests
SLO: 99.9% availability (43.8 min downtime/month)
Alert: <99.9% over 5 minutes

# Latency SLO
SLI: P95 response time
SLO: 95% of requests < 200ms
Alert: P95 > 200ms over 5 minutes

# Error Budget
Monthly Requests: 100M
Error Budget (0.1%): 100K failed requests
Burn Rate Alert: >2x normal rate
```

## 🔗 Quick Links

- [Full Guide](./OBSERVABILITY_STACK_GUIDE.md)
- [PocketBase Deployment Guide](./POCKETBASE_PRODUCTION_DEPLOYMENT_GUIDE.md)
- [Status Script](./observability-status.sh)
- [Startup Script](./start-observability.sh)

---

**💡 Tip**: Keep this file open in a split terminal for quick reference!

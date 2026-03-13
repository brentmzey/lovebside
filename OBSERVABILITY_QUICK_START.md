# 🚀 QUICK START: Enterprise Observability (Phase 1 Lite)

**Goal:** Get enhanced monitoring running locally in <30 minutes  
**Date:** 2026-01-31

---

## What You'll Get

✅ **Enhanced Prometheus** - Database + system metrics  
✅ **Loki + Promtail** - Centralized log aggregation (lightweight alternative to ELK)  
✅ **Updated Grafana** - New datasources and dashboards  
✅ **System Metrics** - CPU, memory, disk via Node Exporter  
✅ **Container Metrics** - Docker stats via cAdvisor  

**NOT included (requires more setup):**
❌ ELK Stack (use Loki instead - lighter)  
❌ Custom SQLite exporter (needs development)  
❌ OpenTelemetry tracing (needs backend code changes)  
❌ AWS integration (needs AWS account setup)  

---

## Quick Setup (3 commands)

```bash
# 1. Navigate to project
cd ~/bside

# 2. Start the enhanced stack
docker-compose -f docker-compose.enhanced-lite.yml up -d

# 3. Wait 60 seconds, then open Grafana
open http://localhost:3000
```

---

## What's New

### New Services

| Service | URL | Purpose |
|---------|-----|---------|
| **Loki** | http://localhost:3100 | Log aggregation |
| **Promtail** | N/A | Log shipping to Loki |
| **Node Exporter** | http://localhost:9100 | System metrics |
| **cAdvisor** | http://localhost:8080 | Container metrics |

### Enhanced Dashboards

Access at http://localhost:3000:

1. **System Overview** - CPU, memory, disk, network
2. **Container Metrics** - Per-container resource usage
3. **Logs Explorer** - Query logs with LogQL
4. **Application Metrics** - Request rates, errors, latency

---

## Verify It's Working

```bash
# Check all services are up
docker-compose ps

# Test Prometheus
curl http://localhost:9090/-/healthy

# Test Loki
curl http://localhost:3100/ready

# Test Node Exporter
curl http://localhost:9100/metrics | head

# View logs in Grafana
open http://localhost:3000/explore
# Select "Loki" datasource
# Query: {service="backend"}
```

---

## Next Steps

Once this is working, you can:

1. **Add Custom Dashboards** - Create business-specific metrics
2. **Set Up Alerts** - Configure Prometheus alerting rules
3. **Add SQLite Monitoring** - Build custom exporter (Phase 1.3)
4. **Enable Tracing** - Add OpenTelemetry to backend (Phase 1.4)
5. **Deploy to AWS** - Use Terraform (Phase 3)

---

## Troubleshooting

### Loki not starting
```bash
# Check logs
docker-compose logs loki

# Common issue: permissions
sudo chown -R $(whoami):$(whoami) ./observability/loki
```

### Can't see logs in Grafana
```bash
# Verify Promtail is running
docker-compose logs promtail

# Check Loki has data
curl "http://localhost:3100/loki/api/v1/labels"
```

### High resource usage
```bash
# Reduce Elasticsearch memory (if using ELK)
# Edit docker-compose: ES_JAVA_OPTS=-Xms256m -Xmx256m

# Or use Loki instead (much lighter)
```

---

See [ENTERPRISE_ROADMAP.md](./.code-hq/ENTERPRISE_ROADMAP.md) for the full plan.

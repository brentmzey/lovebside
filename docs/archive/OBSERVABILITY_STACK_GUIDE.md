# Bside Observability Stack - Complete Guide

## 🎯 Overview

Your complete observability stack is now running with:
- ✅ **Prometheus** - Metrics collection and alerting
- ✅ **Grafana** - Visualization and dashboards  
- ✅ **Jaeger** - Distributed tracing
- ✅ **Loki** (ready) - Log aggregation
- ✅ **Tempo** (ready) - Trace storage
- ✅ **OpenTelemetry** (ready) - Unified telemetry
- ✅ **AlertManager** (ready) - Alert routing
- ✅ **Node Exporter** - System metrics
- ✅ **cAdvisor** - Container metrics
- ✅ **Redis Exporter** - Redis metrics

---

## 🚀 Quick Start

### Check Status
```bash
./observability-status.sh
```

### Start Stack (if not running)
```bash
./start-observability.sh development
```

### Stop Stack
```bash
docker-compose -f docker-compose.observability.yml down
```

### View Logs
```bash
# All services
docker-compose -f docker-compose.observability.yml logs -f

# Specific service
docker-compose -f docker-compose.observability.yml logs -f prometheus
docker-compose -f docker-compose.observability.yml logs -f grafana
docker-compose -f docker-compose.observability.yml logs -f jaeger
```

---

## 📊 Access URLs

### Monitoring & Metrics
| Service | URL | Credentials |
|---------|-----|-------------|
| **Prometheus** | http://localhost:9090 | None |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Node Exporter** | http://localhost:9100 | None |
| **cAdvisor** | http://localhost:8084 | None |

### Tracing
| Service | URL | Purpose |
|---------|-----|---------|
| **Jaeger UI** | http://localhost:16686 | View traces |
| **Jaeger Collector (HTTP)** | localhost:14268 | Send traces |
| **Jaeger Collector (gRPC)** | localhost:14250 | Send traces |
| **Zipkin API** | localhost:9411 | Zipkin-compatible traces |

### Infrastructure
| Service | URL | Purpose |
|---------|-----|---------|
| **Redis UI** | http://localhost:8083 | Redis management |
| **Elasticsearch** | http://localhost:9200 | Search & logs |
| **Kibana** | http://localhost:5601 | Log visualization |

### Application
| Service | URL | Purpose |
|---------|-----|---------|
| **Backend API** | http://localhost:8081 | Main API |
| **PocketBase** | http://localhost:8092 | Database |

---

## 🔧 Environment Configuration

### Development Environment
```bash
./start-observability.sh development
```
- Uses `.env.observability.development`
- Local hostnames
- Debug logging enabled
- No authentication required

### Production Environment
```bash
./start-observability.sh production
```
- Uses `.env.observability.production`
- Production hostnames
- Error-level logging
- Authentication required

### Environment Variables

Edit `.env.observability.development` or `.env.observability.production`:

```bash
# Environment
ENVIRONMENT=development

# Prometheus
PROMETHEUS_RETENTION=30d
PROMETHEUS_SCRAPE_INTERVAL=15s

# Grafana
GF_SECURITY_ADMIN_PASSWORD=admin
GF_SERVER_ROOT_URL=http://localhost:3000

# Jaeger
JAEGER_SAMPLING_RATE=1.0

# Loki
LOKI_RETENTION_PERIOD=168h

# OpenTelemetry
OTEL_RESOURCE_ATTRIBUTES=service.name=bside,environment=development
```

---

## 📈 Grafana Setup

### First Login
1. Go to http://localhost:3000
2. Login with `admin` / `admin`
3. Change password when prompted

### Pre-configured Datasources
Your Grafana instance is automatically configured with:
- ✅ Prometheus (http://prometheus:9090)
- ✅ Loki (http://loki:3100) - when enabled
- ✅ Tempo (http://tempo:3200) - when enabled
- ✅ Jaeger (http://jaeger:16686)

### Import Dashboards

#### Method 1: Import from file
1. Go to **Dashboards** → **Import**
2. Upload JSON file or paste JSON
3. Select datasource: **Prometheus**

#### Method 2: Import by ID
1. Go to **Dashboards** → **Import**  
2. Enter dashboard ID:
   - **1860** - Node Exporter Full
   - **893** - Docker and System Monitoring
   - **14282** - Spring Boot 3.x Statistics
   - **13639** - PocketBase Monitoring
3. Click **Load** → **Import**

### Recommended Dashboards

#### System Monitoring
- **Node Exporter Full** (ID: 1860)
  - CPU, Memory, Disk, Network
  - System load and uptime
  
#### Container Monitoring  
- **Docker Monitoring** (ID: 893)
  - Container metrics from cAdvisor
  - CPU, Memory, Network per container

#### Application Monitoring
- **Spring Boot Dashboard** (ID: 14282)
  - JVM metrics
  - HTTP request rates
  - Database connections

#### Redis Monitoring
- **Redis Dashboard** (ID: 11835)
  - Commands/sec
  - Memory usage
  - Hit rate

---

## 🔍 Prometheus Queries

### Common Queries

#### System Metrics
```promql
# CPU Usage
100 - (avg by (instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# Memory Usage
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes * 100

# Disk Usage
(node_filesystem_size_bytes - node_filesystem_avail_bytes) / node_filesystem_size_bytes * 100
```

#### Container Metrics
```promql
# Container CPU
rate(container_cpu_usage_seconds_total[5m])

# Container Memory
container_memory_usage_bytes / container_spec_memory_limit_bytes * 100

# Container Network
rate(container_network_receive_bytes_total[5m])
```

#### Application Metrics
```promql
# HTTP Request Rate
rate(http_server_requests_seconds_count[5m])

# HTTP Error Rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# HTTP Response Time (p95)
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# Database Connections
hikaricp_connections_active
```

### Create Alerts

1. Go to **Alerting** → **Alert rules** → **New alert rule**
2. Set query:
```promql
# High CPU Alert
avg(rate(node_cpu_seconds_total{mode!="idle"}[5m])) > 0.8

# High Memory Alert  
(node_memory_MemTotal_bytes - node_memory_MemAvailable_bytes) / node_memory_MemTotal_bytes > 0.9

# Container Down
up{job="docker"} == 0
```
3. Configure threshold and notification channel

---

## 🎯 Distributed Tracing with Jaeger

### View Traces
1. Go to http://localhost:16686
2. Select **Service**: bside-backend, bside-pocketbase
3. Click **Find Traces**

### Trace Features
- **Service Map**: Visualize service dependencies
- **Trace Timeline**: See request flow
- **Span Details**: View detailed timing
- **Logs**: View logs attached to spans

### Send Test Traces

#### Using curl
```bash
# Send test span to Jaeger
curl -X POST http://localhost:14268/api/traces \
  -H 'Content-Type: application/json' \
  -d '{
    "data": [{
      "traceID": "1",
      "spanID": "2",
      "operationName": "test-operation",
      "startTime": 1609459200000000,
      "duration": 100000,
      "tags": [
        {"key": "service.name", "value": "test-service"}
      ]
    }]
  }'
```

---

## 🔔 Alerting (AlertManager)

### Enable AlertManager
1. Uncomment AlertManager section in `docker-compose.observability.yml`
2. Restart stack:
```bash
docker-compose -f docker-compose.observability.yml up -d
```

### Configure Alerts

Edit `observability/prometheus/alerts.yml`:

```yaml
groups:
  - name: application_alerts
    interval: 30s
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors/sec"
```

### Configure Notifications

Edit `observability/alertmanager/alertmanager.yml`:

```yaml
route:
  receiver: 'slack'
  
receivers:
  - name: 'slack'
    slack_configs:
      - api_url: 'YOUR_SLACK_WEBHOOK_URL'
        channel: '#alerts'
        text: '{{ .CommonAnnotations.summary }}'
```

---

## 📝 Log Aggregation with Loki

### Enable Loki & Promtail
1. Uncomment Loki and Promtail in `docker-compose.observability.yml`
2. Restart stack

### Query Logs in Grafana
1. Go to **Explore** → Select **Loki** datasource
2. Enter LogQL query:
```logql
# All logs from backend
{container="bside-server"}

# Error logs only
{container="bside-server"} |= "ERROR"

# Filter by level
{container="bside-server"} | json | level="error"

# Rate of errors
rate({container="bside-server"} |= "ERROR"[5m])
```

---

## 🔌 Application Integration

### Spring Boot / Java Integration

#### Add Dependencies (build.gradle)
```gradle
dependencies {
    // Prometheus metrics
    implementation 'io.micrometer:micrometer-registry-prometheus'
    
    // OpenTelemetry tracing
    implementation 'io.opentelemetry:opentelemetry-api:1.32.0'
    implementation 'io.opentelemetry:opentelemetry-sdk:1.32.0'
    implementation 'io.opentelemetry:opentelemetry-exporter-jaeger:1.32.0'
    
    // Distributed tracing
    implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:1.32.0-alpha'
}
```

#### Configuration (application.yml)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:development}

# OpenTelemetry
otel:
  traces:
    exporter: jaeger
  exporter:
    jaeger:
      endpoint: http://localhost:14250
  resource:
    attributes:
      service.name: ${spring.application.name}
      environment: ${ENVIRONMENT:development}
```

#### Custom Metrics
```java
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;

@Component
public class MetricsService {
    
    private final Counter userLoginCounter;
    private final Timer requestTimer;
    
    public MetricsService(MeterRegistry registry) {
        this.userLoginCounter = Counter.builder("user.login.count")
            .description("Number of user logins")
            .tag("environment", System.getenv("ENVIRONMENT"))
            .register(registry);
            
        this.requestTimer = Timer.builder("api.request.duration")
            .description("API request duration")
            .register(registry);
    }
    
    public void recordLogin() {
        userLoginCounter.increment();
    }
    
    public void recordRequest(Runnable action) {
        requestTimer.record(action);
    }
}
```

#### Custom Tracing
```java
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@Service
public class UserService {
    
    private final Tracer tracer;
    
    public UserService(Tracer tracer) {
        this.tracer = tracer;
    }
    
    public User getUserById(String userId) {
        Span span = tracer.spanBuilder("getUserById")
            .setAttribute("user.id", userId)
            .startSpan();
            
        try (Scope scope = span.makeCurrent()) {
            // Your business logic
            User user = userRepository.findById(userId);
            span.setAttribute("user.found", user != null);
            return user;
        } finally {
            span.end();
        }
    }
}
```

### Node.js / React Integration

#### Install packages
```bash
npm install prom-client @opentelemetry/api @opentelemetry/sdk-node @opentelemetry/auto-instrumentations-node
```

#### Metrics (Express)
```javascript
const promClient = require('prom-client');
const express = require('express');

// Create metrics registry
const register = new promClient.Registry();

// Collect default metrics
promClient.collectDefaultMetrics({ register });

// Custom metrics
const httpRequestDuration = new promClient.Histogram({
  name: 'http_request_duration_seconds',
  help: 'Duration of HTTP requests in seconds',
  labelNames: ['method', 'route', 'status'],
  registers: [register]
});

// Middleware
app.use((req, res, next) => {
  const start = Date.now();
  res.on('finish', () => {
    const duration = (Date.now() - start) / 1000;
    httpRequestDuration
      .labels(req.method, req.route?.path || req.path, res.statusCode)
      .observe(duration);
  });
  next();
});

// Metrics endpoint
app.get('/metrics', async (req, res) => {
  res.set('Content-Type', register.contentType);
  res.end(await register.metrics());
});
```

#### Tracing (Node.js)
```javascript
const { NodeSDK } = require('@opentelemetry/sdk-node');
const { getNodeAutoInstrumentations } = require('@opentelemetry/auto-instrumentations-node');
const { JaegerExporter } = require('@opentelemetry/exporter-jaeger');

const sdk = new NodeSDK({
  traceExporter: new JaegerExporter({
    endpoint: 'http://localhost:14268/api/traces',
  }),
  instrumentations: [getNodeAutoInstrumentations()],
  serviceName: 'bside-frontend',
});

sdk.start();
```

---

## 🛠️ Troubleshooting

### Container Won't Start
```bash
# Check logs
docker-compose -f docker-compose.observability.yml logs [service]

# Check container status
docker ps -a | grep [service]

# Restart service
docker-compose -f docker-compose.observability.yml restart [service]
```

### Prometheus Not Scraping Targets
1. Check Prometheus targets: http://localhost:9090/targets
2. Verify service is exposing metrics endpoint
3. Check network connectivity:
```bash
docker exec bside-prometheus wget -O- http://server:8080/actuator/prometheus
```

### Grafana Can't Connect to Datasource
1. Go to **Configuration** → **Data sources**
2. Click datasource → **Test**
3. Check error message
4. Verify datasource URL uses container name (e.g., `http://prometheus:9090`)

### Jaeger Not Receiving Traces
1. Check Jaeger collector logs:
```bash
docker-compose -f docker-compose.observability.yml logs jaeger
```
2. Verify application is sending to correct endpoint
3. Test with curl:
```bash
curl -v http://localhost:14268/api/traces
```

### High Resource Usage
```bash
# Check resource usage
docker stats

# Reduce Prometheus retention
# Edit docker-compose.observability.yml:
# --storage.tsdb.retention.time=15d

# Limit container resources
# Add to service in docker-compose.observability.yml:
# deploy:
#   resources:
#     limits:
#       cpus: '0.5'
#       memory: 512M
```

---

## 📚 Best Practices

### 1. Metrics
- ✅ Use descriptive metric names (e.g., `http_requests_total`)
- ✅ Add labels for dimensions (e.g., `method`, `status`, `environment`)
- ✅ Use appropriate metric types (Counter, Gauge, Histogram)
- ❌ Don't use high-cardinality labels (e.g., user IDs, timestamps)

### 2. Tracing
- ✅ Trace critical user journeys
- ✅ Add context with span attributes
- ✅ Use sampling in production (10-20%)
- ✅ Include error information in spans
- ❌ Don't trace every single request (performance impact)

### 3. Logging
- ✅ Use structured logging (JSON format)
- ✅ Include trace IDs in logs
- ✅ Set appropriate log levels
- ✅ Use log aggregation (Loki)
- ❌ Don't log sensitive information (passwords, tokens)

### 4. Alerts
- ✅ Alert on symptoms, not causes
- ✅ Define clear SLOs/SLIs
- ✅ Use appropriate thresholds
- ✅ Test alert rules
- ❌ Don't create alert fatigue

---

## 🔐 Security Considerations

### Production Checklist
- [ ] Change default Grafana password
- [ ] Enable authentication on Prometheus
- [ ] Use HTTPS for all endpoints
- [ ] Restrict network access
- [ ] Enable AlertManager authentication
- [ ] Encrypt sensitive data in alerts
- [ ] Regular security updates

### Network Security
```yaml
# Add to docker-compose.observability.yml
services:
  prometheus:
    networks:
      - monitoring_internal
    # Only expose to internal network

networks:
  monitoring_internal:
    internal: true  # No external access
```

---

## 📖 Additional Resources

### Documentation
- Prometheus: https://prometheus.io/docs/
- Grafana: https://grafana.com/docs/
- Jaeger: https://www.jaegertracing.io/docs/
- OpenTelemetry: https://opentelemetry.io/docs/
- Loki: https://grafana.com/docs/loki/

### Community Dashboards
- Grafana Dashboards: https://grafana.com/grafana/dashboards/
- Prometheus Exporters: https://prometheus.io/docs/instrumenting/exporters/

### Tutorials
- Prometheus Best Practices: https://prometheus.io/docs/practices/
- Grafana Tutorials: https://grafana.com/tutorials/
- OpenTelemetry Demo: https://github.com/open-telemetry/opentelemetry-demo

---

## 🎉 Summary

Your observability stack is now fully configured and running with:

✅ **Metrics Collection**: Prometheus scraping all services  
✅ **Visualization**: Grafana with pre-configured datasources  
✅ **Distributed Tracing**: Jaeger collecting traces  
✅ **System Monitoring**: Node Exporter + cAdvisor  
✅ **Container Monitoring**: Docker metrics  
✅ **Application Metrics**: Spring Boot actuator  
✅ **Infrastructure Monitoring**: Redis, Elasticsearch  

### Quick Links
- Status Check: `./observability-status.sh`
- Start Stack: `./start-observability.sh development`
- Grafana: http://localhost:3000
- Prometheus: http://localhost:9090
- Jaeger: http://localhost:16686

**🚀 Your complete observability platform is ready for development and production!**

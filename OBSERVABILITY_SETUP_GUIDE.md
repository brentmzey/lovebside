# Full Stack Observability Guide

## Complete Setup for Grafana, Prometheus, OpenTelemetry, Jaeger, Loki & More

**Environment-Agnostic Configuration for Development, Staging & Production**

---

## Table of Contents

1. [Quick Start](#quick-start)
2. [Architecture Overview](#architecture-overview)
3. [Components](#components)
4. [Environment Configuration](#environment-configuration)
5. [Starting the Stack](#starting-the-stack)
6. [Accessing Services](#accessing-services)
7. [Instrumentation Guide](#instrumentation-guide)
8. [Troubleshooting](#troubleshooting)

---

## Quick Start

### Prerequisites
```bash
# Required tools
docker --version  # Docker 20.10+
docker-compose --version  # Docker Compose 2.0+
```

### Start Everything
```bash
# Development environment
./start-observability.sh development

# Production environment
./start-observability.sh production

# With custom environment file
./start-observability.sh production /path/to/.env
```

### Verify Services
```bash
# Check all services are running
docker-compose -f docker-compose.observability.yml ps

# View logs
docker-compose -f docker-compose.observability.yml logs -f
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Application Layer                            │
│  (Frontend, Backend, PocketBase, Services)                       │
└────────────┬─────────────────────────────────────┬──────────────┘
             │                                     │
             │ Traces, Metrics, Logs               │
             ▼                                     ▼
┌────────────────────────────────────────────────────────────────┐
│               OpenTelemetry Collector                           │
│  - Receives telemetry from all sources                          │
│  - Processes, filters, enriches data                            │
│  - Routes to appropriate backends                               │
└────────┬──────────┬──────────┬──────────┬───────────┬──────────┘
         │          │          │          │           │
         ▼          ▼          ▼          ▼           ▼
┌────────────┐ ┌─────────┐ ┌──────────┐ ┌────────┐ ┌───────────┐
│ Prometheus │ │  Jaeger │ │  Tempo   │ │  Loki  │ │ Backends  │
│  (Metrics) │ │ (Traces)│ │ (Traces) │ │ (Logs) │ │  (More)   │
└────────────┘ └─────────┘ └──────────┘ └────────┘ └───────────┘
         │          │          │          │
         └──────────┴──────────┴──────────┘
                     │
                     ▼
              ┌──────────────┐
              │   Grafana    │
              │ (Dashboards) │
              └──────────────┘
                     │
                     ▼
              ┌──────────────┐
              │ AlertManager │
              │   (Alerts)   │
              └──────────────┘
```

---

## Components

### 1. **OpenTelemetry Collector**
- **Port**: 4317 (gRPC), 4318 (HTTP), 8888 (metrics), 13133 (health)
- **Purpose**: Central telemetry collection and routing
- **Features**:
  - Protocol translation (OTLP, Jaeger, Zipkin, Prometheus)
  - Data processing (filtering, batching, enrichment)
  - Multi-backend export
- **Config**: `observability/otel/otel-collector-config.yaml`

### 2. **Prometheus**
- **Port**: 9090
- **Purpose**: Time-series metrics storage and querying
- **Features**:
  - Pull-based metric collection
  - PromQL query language
  - Alerting rules
  - Long-term storage
- **Config**: `observability/prometheus/prometheus.yml`
- **Alerts**: `observability/prometheus/alerts.yml`

### 3. **Grafana**
- **Port**: 3000
- **Purpose**: Unified visualization and dashboards
- **Default Login**: admin / admin123 (change in production!)
- **Features**:
  - Multi-datasource support
  - Custom dashboards
  - Alerting and notifications
  - User management
- **Config**: `observability/grafana/provisioning/`

### 4. **Jaeger**
- **Port**: 16686 (UI), 14250 (gRPC), 14268 (HTTP)
- **Purpose**: Distributed tracing UI and storage
- **Features**:
  - Trace visualization
  - Service dependency graphs
  - Performance analysis
  - Span search
- **Storage**: In-memory (dev) or Cassandra/Elasticsearch (prod)

### 5. **Loki**
- **Port**: 3100
- **Purpose**: Log aggregation and querying
- **Features**:
  - Label-based log indexing
  - LogQL query language
  - Low storage overhead
  - Grafana integration
- **Config**: `observability/loki/loki-config.yaml`

### 6. **Promtail**
- **Port**: 9080
- **Purpose**: Log shipper for Loki
- **Features**:
  - Docker log collection
  - File-based log tailing
  - Label enrichment
  - Multi-tenant support
- **Config**: `observability/promtail/promtail-config.yaml`

### 7. **Tempo**
- **Port**: 3200 (HTTP), 4317 (OTLP gRPC)
- **Purpose**: High-volume distributed tracing backend
- **Features**:
  - Cost-effective trace storage
  - Object storage backend (S3, GCS, Azure)
  - TraceQL query language
  - Grafana integration
- **Config**: `observability/tempo/tempo.yaml`

### 8. **AlertManager**
- **Port**: 9093
- **Purpose**: Alert routing and management
- **Features**:
  - Alert deduplication
  - Grouping and routing
  - Multiple notification channels (Slack, email, PagerDuty)
  - Silencing rules
- **Config**: `observability/alertmanager/alertmanager.yml`

---

## Environment Configuration

### Environment Variables

#### Development (`.env.observability.development`)
```bash
# Environment
ENVIRONMENT=development
LOG_LEVEL=debug

# Grafana
GRAFANA_ADMIN_PASSWORD=admin123
GF_SECURITY_ALLOW_EMBEDDING=true

# Prometheus
PROMETHEUS_RETENTION_TIME=7d
PROMETHEUS_SCRAPE_INTERVAL=15s

# Loki
LOKI_RETENTION_PERIOD=168h
```

#### Production (`.env.observability.production`)
```bash
# Environment
ENVIRONMENT=production
LOG_LEVEL=info

# Grafana
GRAFANA_ADMIN_PASSWORD=${SECURE_GRAFANA_PASSWORD}
GF_SECURITY_ALLOW_EMBEDDING=false

# Prometheus
PROMETHEUS_RETENTION_TIME=30d
PROMETHEUS_SCRAPE_INTERVAL=30s

# Loki
LOKI_RETENTION_PERIOD=720h
```

### Dynamic Environment Detection

The stack automatically detects and configures based on:
1. `.env.observability.{environment}` files
2. Environment variables
3. Docker labels
4. Hostname patterns

---

## Starting the Stack

### Option 1: Using the Startup Script (Recommended)

```bash
# Development with hot reload
./start-observability.sh development

# Production with optimizations
./start-observability.sh production

# Custom environment file
./start-observability.sh staging /path/to/.env.staging
```

**Script Features:**
- ✅ Environment validation
- ✅ Dependency checking
- ✅ Automatic directory creation
- ✅ Health checks
- ✅ Service verification
- ✅ Helpful error messages

### Option 2: Direct Docker Compose

```bash
# Load environment
export $(cat .env.observability.development | xargs)

# Start all services
docker-compose -f docker-compose.observability.yml up -d

# Start specific services
docker-compose -f docker-compose.observability.yml up -d grafana prometheus

# View logs
docker-compose -f docker-compose.observability.yml logs -f

# Stop all services
docker-compose -f docker-compose.observability.yml down

# Stop and remove volumes (WARNING: deletes data)
docker-compose -f docker-compose.observability.yml down -v
```

---

## Accessing Services

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | admin / admin123 |
| **Prometheus** | http://localhost:9090 | None |
| **Jaeger UI** | http://localhost:16686 | None |
| **Loki** | http://localhost:3100 | None |
| **AlertManager** | http://localhost:9093 | None |
| **OTEL Collector** | http://localhost:8888/metrics | None |

### Grafana Initial Setup

1. **Login**: http://localhost:3000
   - Username: `admin`
   - Password: `admin123` (dev) or from env var (prod)

2. **Change Password** (Production Required)
   - Settings → Profile → Change Password

3. **Verify Datasources**
   - Configuration → Data Sources
   - Should see: Prometheus, Loki, Jaeger, Tempo

4. **Import Dashboards**
   - Dashboards → Import
   - Pre-configured dashboards in `observability/grafana/dashboards/`

---

## Instrumentation Guide

### Backend (Node.js / TypeScript)

#### 1. Install OpenTelemetry SDK

```bash
npm install @opentelemetry/api \
  @opentelemetry/sdk-node \
  @opentelemetry/auto-instrumentations-node \
  @opentelemetry/exporter-trace-otlp-grpc \
  @opentelemetry/exporter-metrics-otlp-grpc
```

#### 2. Create Tracing Configuration

**File**: `src/observability/tracing.ts`

```typescript
import { NodeSDK } from '@opentelemetry/sdk-node';
import { getNodeAutoInstrumentations } from '@opentelemetry/auto-instrumentations-node';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-grpc';
import { OTLPMetricExporter } from '@opentelemetry/exporter-metrics-otlp-grpc';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';

const environment = process.env.ENVIRONMENT || 'development';
const serviceName = process.env.SERVICE_NAME || 'bside-backend';
const serviceVersion = process.env.SERVICE_VERSION || '1.0.0';

// Configure OpenTelemetry
const sdk = new NodeSDK({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: serviceName,
    [SemanticResourceAttributes.SERVICE_VERSION]: serviceVersion,
    [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: environment,
  }),
  traceExporter: new OTLPTraceExporter({
    url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT || 'http://localhost:4317',
  }),
  metricReader: new OTLPMetricExporter({
    url: process.env.OTEL_EXPORTER_OTLP_ENDPOINT || 'http://localhost:4317',
  }),
  instrumentations: [
    getNodeAutoInstrumentations({
      '@opentelemetry/instrumentation-fs': { enabled: false },
    }),
  ],
});

// Start the SDK
sdk.start();

// Graceful shutdown
process.on('SIGTERM', () => {
  sdk.shutdown()
    .then(() => console.log('Tracing terminated'))
    .catch((error) => console.error('Error terminating tracing', error))
    .finally(() => process.exit(0));
});

export default sdk;
```

#### 3. Initialize in Application

**File**: `src/index.ts`

```typescript
// MUST be first import
import './observability/tracing';

import express from 'express';
import { trace, context } from '@opentelemetry/api';

const app = express();
const tracer = trace.getTracer('bside-backend');

app.get('/api/users', async (req, res) => {
  // Create custom span
  const span = tracer.startSpan('get-users');
  
  try {
    // Your business logic
    const users = await fetchUsers();
    
    // Add span attributes
    span.setAttribute('user.count', users.length);
    span.setStatus({ code: 0 }); // OK
    
    res.json(users);
  } catch (error) {
    span.recordException(error);
    span.setStatus({ code: 2, message: error.message }); // ERROR
    res.status(500).json({ error: 'Internal server error' });
  } finally {
    span.end();
  }
});

app.listen(3001, () => {
  console.log('Server running on port 3001');
});
```

### Frontend (React / SvelteKit)

#### 1. Install OpenTelemetry Web SDK

```bash
npm install @opentelemetry/api \
  @opentelemetry/sdk-trace-web \
  @opentelemetry/instrumentation-document-load \
  @opentelemetry/instrumentation-fetch \
  @opentelemetry/exporter-trace-otlp-http
```

#### 2. Configure Web Tracing

**File**: `src/lib/observability/tracing.ts`

```typescript
import { WebTracerProvider } from '@opentelemetry/sdk-trace-web';
import { DocumentLoadInstrumentation } from '@opentelemetry/instrumentation-document-load';
import { FetchInstrumentation } from '@opentelemetry/instrumentation-fetch';
import { registerInstrumentations } from '@opentelemetry/instrumentation';
import { OTLPTraceExporter } from '@opentelemetry/exporter-trace-otlp-http';
import { Resource } from '@opentelemetry/resources';
import { SemanticResourceAttributes } from '@opentelemetry/semantic-conventions';
import { BatchSpanProcessor } from '@opentelemetry/sdk-trace-base';

const environment = import.meta.env.MODE || 'development';

const provider = new WebTracerProvider({
  resource: new Resource({
    [SemanticResourceAttributes.SERVICE_NAME]: 'bside-frontend',
    [SemanticResourceAttributes.SERVICE_VERSION]: '1.0.0',
    [SemanticResourceAttributes.DEPLOYMENT_ENVIRONMENT]: environment,
  }),
});

// Configure exporter
const exporter = new OTLPTraceExporter({
  url: import.meta.env.VITE_OTEL_EXPORTER_URL || 'http://localhost:4318/v1/traces',
});

provider.addSpanProcessor(new BatchSpanProcessor(exporter));
provider.register();

// Register instrumentations
registerInstrumentations({
  instrumentations: [
    new DocumentLoadInstrumentation(),
    new FetchInstrumentation({
      propagateTraceHeaderCorsUrls: /.*/,
      clearTimingResources: true,
    }),
  ],
});

export default provider;
```

### PocketBase Integration

#### Custom Hook Script

**File**: `pocketbase/pb_hooks/observability.pb.js`

```javascript
// Basic request logging to stdout (picked up by Promtail)
onBeforeServe((e) => {
  e.app.use((next) => {
    return (c) => {
      const start = Date.now();
      const result = next(c);
      const duration = Date.now() - start;
      
      // Structured logging
      console.log(JSON.stringify({
        timestamp: new Date().toISOString(),
        service: 'pocketbase',
        method: c.request().method,
        path: c.request().url.pathname,
        status: c.response().status,
        duration_ms: duration,
        user_id: c.get('authRecord')?.id || 'anonymous',
      }));
      
      return result;
    };
  });
});
```

---

## Troubleshooting

### Services Won't Start

```bash
# Check Docker is running
docker info

# Check port conflicts
netstat -tuln | grep -E '(3000|9090|16686|4317)'

# View detailed logs
docker-compose -f docker-compose.observability.yml logs

# Recreate services
docker-compose -f docker-compose.observability.yml down
docker-compose -f docker-compose.observability.yml up -d --force-recreate
```

### No Data in Grafana

1. **Check Datasource Connection**
   - Grafana → Configuration → Data Sources
   - Click "Test" on each datasource

2. **Verify Application is Sending Data**
   ```bash
   # Check OpenTelemetry Collector metrics
   curl http://localhost:8888/metrics | grep otelcol_receiver
   
   # Check Prometheus targets
   curl http://localhost:9090/api/v1/targets
   ```

3. **Check Application Configuration**
   - Verify `OTEL_EXPORTER_OTLP_ENDPOINT` environment variable
   - Ensure application can reach collector (network/firewall)

### High Memory Usage

```bash
# Check resource usage
docker stats

# Adjust retention periods in .env files
PROMETHEUS_RETENTION_TIME=3d
LOKI_RETENTION_PERIOD=72h

# Restart with new config
./start-observability.sh development
```

### Traces Not Appearing

1. **Verify Instrumentation**
   - Check traces are being created in application
   - Verify exporter endpoint is correct

2. **Check Collector**
   ```bash
   docker-compose logs otel-collector | grep -i trace
   ```

3. **Check Jaeger/Tempo**
   ```bash
   # Jaeger API
   curl http://localhost:16686/api/services
   
   # Tempo API  
   curl http://localhost:3200/api/search
   ```

---

## Best Practices

### 1. **Sampling**
- Use probabilistic sampling in production
- Configure in OpenTelemetry Collector
- Balance cost vs. visibility

### 2. **Cardinality**
- Avoid high-cardinality labels (user IDs, timestamps)
- Use label aggregation
- Monitor Prometheus TSDB size

### 3. **Alerting**
- Start with critical alerts only
- Tune thresholds based on baselines
- Use AlertManager for deduplication

### 4. **Security**
- Change default passwords
- Use TLS in production
- Implement authentication for all services
- Network isolation (separate Docker network)

### 5. **Costs**
- Monitor storage growth
- Adjust retention periods
- Use aggregation rules
- Consider sampling strategies

---

## Production Deployment

### Kubernetes/Docker Swarm

Use the provided configurations as base and:
- Add persistent volumes for data
- Configure ingress/load balancers
- Implement TLS termination
- Use secrets management
- Enable high availability (multiple replicas)

### Cloud Providers

- **AWS**: Consider Amazon Managed Service for Prometheus, Grafana
- **GCP**: Use Cloud Monitoring, Cloud Trace
- **Azure**: Azure Monitor, Application Insights

### Managed Services

- **Grafana Cloud**: Hosted Grafana, Loki, Tempo
- **Datadog**: All-in-one observability
- **New Relic**: APM and infrastructure monitoring

---

## Additional Resources

- [OpenTelemetry Docs](https://opentelemetry.io/docs/)
- [Prometheus Best Practices](https://prometheus.io/docs/practices/naming/)
- [Grafana Tutorials](https://grafana.com/tutorials/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Loki Documentation](https://grafana.com/docs/loki/latest/)

---

**Document Version**: 1.0  
**Last Updated**: February 1, 2026  
**Status**: ✅ Production Ready

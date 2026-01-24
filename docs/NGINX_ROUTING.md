# Nginx Configuration Guide

## Overview

This Nginx configuration provides **intelligent routing** between PocketBase and Ktor, with production-ready features including load balancing, WebSocket support, and security hardening.

---

## Routing Strategy

### Path-Based Routing

```
/api/pb/*        → PocketBase (CRUD, Auth, Real-time)
/api/v1/*        → Ktor Backend (Business Logic, Jobs)
/_/*             → PocketBase Admin UI
/*               → Ktor Backend (Fallback)
```

### Why This Design?

- **PocketBase** handles:
  - User authentication
  - Direct database CRUD
  - Real-time subscriptions
  - File storage
  
- **Ktor** handles:
  - Matching algorithms
  - Complex business logic
  - Background jobs
  - External API integrations

---

## Key Features

### 1. WebSocket Support ✅

**For**: Real-time messaging subscriptions

```nginx
proxy_set_header Upgrade $http_upgrade;
proxy_set_header Connection "upgrade";
```

### 2. File Upload Optimization ✅

**Limits**: 50MB per file, optimized timeouts

```nginx
location /api/pb/files/ {
  client_max_body_size 50M;
  proxy_read_timeout 600s;  # 10 min for large uploads
}
```

### 3. Rate Limiting ✅

**Protects**: Against DDoS and abuse

- API calls: 10 req/sec (burst 20)
- File uploads: 2 req/sec (burst 5)

### 4. Load Balancing ✅

**Upstreams**: Health checks with failover

```nginx
upstream pocketbase {
  server pocketbase:8090 max_fails=3 fail_timeout=30s;
}
```

### 5. Security Headers ✅

```nginx
X-Frame-Options: SAMEORIGIN
X-Content-Type-Options: nosniff
X-XSS-Protection: 1; mode=block
```

---

## Environment Configuration

### Local Development

```nginx
server_name localhost bside.local;
listen 80;
```

### Production (with SSL)

```nginx
server_name bside.app www.bside.app;
listen 443 ssl http2;
ssl_certificate /etc/nginx/ssl/bside.app.crt;
ssl_certificate_key /etc/nginx/ssl/bside.app.key;
```

**Note**: Uncomment HTTPS blocks in production and configure SSL certificates.

---

## Testing

### 1. Validate Configuration

```bash
# In Docker
docker-compose exec nginx nginx -t

# Or locally
nginx -t -c nginx/nginx.conf
```

### 2. Test Routing

```bash
# PocketBase health
curl http://localhost/api/pb/health

# Ktor health
curl http://localhost/api/v1/health

# Admin UI
curl http://localhost/_/
```

### 3. Test WebSockets

```javascript
// In browser console
const ws = new WebSocket('ws://localhost/api/pb/realtime');
ws.onopen = () => console.log('Connected');
```

### 4. Test File Upload

```bash
curl -X POST http://localhost/api/pb/files \
  -F "file=@test.jpg" \
  -H "Authorization: Bearer <token>"
```

---

## Performance Tuning

### Connection Pooling

```nginx
upstream pocketbase {
  keepalive 32;  # Keep 32 connections alive
}
```

### Gzip Compression

```nginx
gzip on;
gzip_types application/json text/javascript;
```

### Caching (Future Enhancement)

```nginx
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=api_cache:10m;
```

---

## Monitoring

### Access Logs

```bash
# View real-time access logs
docker-compose logs -f nginx

# Analyze request times
tail -f /var/log/nginx/access.log | grep "rt="
```

### Metrics Tracked

- `rt`: Total request time
- `uct`: Upstream connect time
- `uht`: Upstream header time
- `urt`: Upstream response time

---

## Troubleshooting

### "502 Bad Gateway"

→ Upstream service (PocketBase/Ktor) is down. Check:

```bash
docker-compose ps
docker-compose logs pocketbase
docker-compose logs server
```

### "413 Request Entity Too Large"

→ File upload exceeds limit. Check `client_max_body_size`.

### "504 Gateway Timeout"

→ Upstream took too long. Increase `proxy_read_timeout`.

### WebSocket Not Working

→ Verify `Upgrade` headers are set correctly.

---

## Production Checklist

- [ ] Update `server_name` to production domain
- [ ] Uncomment HTTPS blocks
- [ ] Configure SSL certificates
- [ ] Set up HTTP → HTTPS redirect
- [ ] Configure firewall rules
- [ ] Set up monitoring/alerts
- [ ] Test all routes with production traffic
- [ ] Configure CDN (CloudFlare, etc.)

---

## Related Files

- `docker-compose.yml` - Service definitions
- `docs/RUNNING.md` - How to run the stack
- `.code-hq/STORIES.md` - Story 3 acceptance criteria

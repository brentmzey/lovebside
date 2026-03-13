#!/bin/bash

# Grafana Quick Setup Script
# Automates datasource creation via API

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║     🚀 Grafana Quick Setup - Automated Configuration         ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

GRAFANA_URL="http://localhost:3000"
GRAFANA_USER="admin"
GRAFANA_PASS="admin"

echo "⏳ Waiting for Grafana to be ready..."
until curl -s "$GRAFANA_URL/api/health" > /dev/null 2>&1; do
  echo -n "."
  sleep 2
done
echo " ✅ Grafana is ready!"
echo ""

# Add Prometheus Datasource
echo "📊 Adding Prometheus datasource..."
curl -s -X POST "$GRAFANA_URL/api/datasources" \
  -u "$GRAFANA_USER:$GRAFANA_PASS" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Prometheus",
    "type": "prometheus",
    "url": "http://prometheus:9090",
    "access": "proxy",
    "isDefault": true,
    "jsonData": {
      "timeInterval": "15s"
    }
  }' | jq -r '.message // .name' && echo " ✅ Prometheus added"

echo ""

# Add Loki Datasource
echo "📝 Adding Loki datasource..."
curl -s -X POST "$GRAFANA_URL/api/datasources" \
  -u "$GRAFANA_USER:$GRAFANA_PASS" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Loki",
    "type": "loki",
    "url": "http://loki:3100",
    "access": "proxy",
    "jsonData": {
      "maxLines": 1000
    }
  }' | jq -r '.message // .name' && echo " ✅ Loki added"

echo ""
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║                                                               ║"
echo "║  ✅ Grafana is configured!                                   ║"
echo "║                                                               ║"
echo "║  Next steps:                                                 ║"
echo "║  1. Open http://localhost:3000                               ║"
echo "║  2. Login: admin / admin                                     ║"
echo "║  3. Import dashboards (IDs: 1860, 763, 14282)                ║"
echo "║                                                               ║"
echo "║  Or follow the walkthrough:                                  ║"
echo "║  cat .code-hq/OPTION_B_WALKTHROUGH.md                        ║"
echo "║                                                               ║"
echo "╚═══════════════════════════════════════════════════════════════╝"

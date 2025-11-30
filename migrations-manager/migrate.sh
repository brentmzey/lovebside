#!/bin/bash

# Migration helper script for quick access
# Usage: ./migrate.sh [command] [args]

set -e

cd "$(dirname "$0")"

case "$1" in
  status)
    npm run migrate:status
    ;;
  up)
    npm run migrate:up
    ;;
  down)
    npm run migrate:down
    ;;
  create)
    if [ -z "$2" ]; then
      echo "Error: Migration name required"
      echo "Usage: ./migrate.sh create <name>"
      exit 1
    fi
    npm run migrate:create "$2"
    ;;
  *)
    npm run migrate
    ;;
esac

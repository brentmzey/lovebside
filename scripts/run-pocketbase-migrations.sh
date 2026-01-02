#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MANAGER_DIR="$ROOT_DIR/pocketbase/migrations-manager"
DEFAULT_SCRIPT="migrate"

usage() {
  cat <<'EOF'
Usage: scripts/run-pocketbase-migrations.sh [npm-script] [args...]

Examples:
  scripts/run-pocketbase-migrations.sh           # Run pending migrations (npm run migrate)
  scripts/run-pocketbase-migrations.sh migrate:status
  scripts/run-pocketbase-migrations.sh migrate:create "add prompts"
EOF
}

if [[ ${1:-} == "-h" || ${1:-} == "--help" ]]; then
  usage
  exit 0
fi

if [[ ! -d "$MANAGER_DIR" ]]; then
  echo "migrations-manager/ directory not found" >&2
  exit 1
fi

pushd "$MANAGER_DIR" >/dev/null

if [[ ! -d node_modules ]]; then
  echo "Installing migrations-manager dependencies..."
  npm install --no-audit --no-fund
fi

SCRIPT_NAME=${1:-$DEFAULT_SCRIPT}
if [[ $# -gt 0 ]]; then
  shift
fi

echo "Running npm script: $SCRIPT_NAME"
npm run "$SCRIPT_NAME" -- "$@"

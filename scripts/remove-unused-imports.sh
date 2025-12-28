#!/usr/bin/env bash
# Remove unused imports from Kotlin source files using ktlint

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

echo "🧹 Removing unused imports from Kotlin files..."

# Run ktlint with format on all source directories
find shared/src composeApp/src server/src bside-api/src -name "*.kt" -type f 2>/dev/null | while read -r file; do
    ktlint --format "$file" 2>&1 | grep -v "^$" || true
done

echo "✅ Unused imports removed!"
echo ""
echo "Run 'git diff' to see changes"

#!/usr/bin/env bash
# Version Bump Script
# Usage: ./scripts/bump-version.sh [major|minor|patch]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Current version from gradle.properties or default
CURRENT_VERSION=$(grep "^version=" "$ROOT_DIR/gradle.properties" 2>/dev/null | cut -d'=' -f2 || echo "0.1.0")

echo -e "${BLUE}📦 BSide Version Manager${NC}"
echo ""
echo -e "${YELLOW}Current version: ${CURRENT_VERSION}${NC}"
echo ""

# Parse version
IFS='.' read -ra VERSION_PARTS <<< "$CURRENT_VERSION"
MAJOR="${VERSION_PARTS[0]}"
MINOR="${VERSION_PARTS[1]}"
PATCH="${VERSION_PARTS[2]}"

# Determine bump type
BUMP_TYPE="${1:-patch}"

case "$BUMP_TYPE" in
  major)
    MAJOR=$((MAJOR + 1))
    MINOR=0
    PATCH=0
    ;;
  minor)
    MINOR=$((MINOR + 1))
    PATCH=0
    ;;
  patch)
    PATCH=$((PATCH + 1))
    ;;
  *)
    echo -e "${RED}❌ Invalid bump type: $BUMP_TYPE${NC}"
    echo -e "Usage: $0 [major|minor|patch]"
    exit 1
    ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"

echo -e "${GREEN}New version: ${NEW_VERSION}${NC}"
echo ""

# Confirm
read -p "Proceed with version bump? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  echo -e "${YELLOW}⚠️  Aborted${NC}"
  exit 0
fi

echo -e "${BLUE}📝 Updating version files...${NC}"

# Update gradle.properties
if grep -q "^version=" "$ROOT_DIR/gradle.properties"; then
  sed -i.bak "s/^version=.*/version=$NEW_VERSION/" "$ROOT_DIR/gradle.properties"
  rm -f "$ROOT_DIR/gradle.properties.bak"
  echo -e "${GREEN}✓ Updated gradle.properties${NC}"
else
  echo "version=$NEW_VERSION" >> "$ROOT_DIR/gradle.properties"
  echo -e "${GREEN}✓ Added version to gradle.properties${NC}"
fi

# Update IMPLEMENTATION_STATUS.md
if [ -f "$ROOT_DIR/IMPLEMENTATION_STATUS.md" ]; then
  TODAY=$(date +%Y-%m-%d)
  sed -i.bak "s/^**Version:**.*/**Version:** ${NEW_VERSION}/" "$ROOT_DIR/IMPLEMENTATION_STATUS.md" || true
  sed -i.bak "s/^**Last Updated:**.*/**Last Updated:** ${TODAY}/" "$ROOT_DIR/IMPLEMENTATION_STATUS.md" || true
  rm -f "$ROOT_DIR/IMPLEMENTATION_STATUS.md.bak"
  echo -e "${GREEN}✓ Updated IMPLEMENTATION_STATUS.md${NC}"
fi

# Update README.md
if [ -f "$ROOT_DIR/README.md" ]; then
  sed -i.bak "s/Version [0-9]\+\.[0-9]\+\.[0-9]\+/Version ${NEW_VERSION}/g" "$ROOT_DIR/README.md" || true
  rm -f "$ROOT_DIR/README.md.bak"
  echo -e "${GREEN}✓ Updated README.md${NC}"
fi

# Update package.json if exists
if [ -f "$ROOT_DIR/package.json" ]; then
  if command -v jq &> /dev/null; then
    jq ".version = \"$NEW_VERSION\"" "$ROOT_DIR/package.json" > "$ROOT_DIR/package.json.tmp"
    mv "$ROOT_DIR/package.json.tmp" "$ROOT_DIR/package.json"
    echo -e "${GREEN}✓ Updated package.json${NC}"
  else
    sed -i.bak "s/\"version\": \".*\"/\"version\": \"$NEW_VERSION\"/" "$ROOT_DIR/package.json"
    rm -f "$ROOT_DIR/package.json.bak"
    echo -e "${GREEN}✓ Updated package.json${NC}"
  fi
fi

echo ""
echo -e "${BLUE}📝 Creating git commit...${NC}"

# Git commit
git add -A
git commit -m "chore: Bump version to ${NEW_VERSION}

- Updated version in gradle.properties
- Updated version in documentation
- Prepared for release

[skip ci]"

echo -e "${GREEN}✓ Committed changes${NC}"
echo ""

# Create tag option
read -p "Create git tag v${NEW_VERSION}? (y/N) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  git tag -a "v${NEW_VERSION}" -m "Release version ${NEW_VERSION}"
  echo -e "${GREEN}✓ Created tag v${NEW_VERSION}${NC}"
  echo ""
  echo -e "${YELLOW}To push tag and trigger release:${NC}"
  echo -e "  ${BLUE}git push origin v${NEW_VERSION}${NC}"
fi

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Version bumped from ${CURRENT_VERSION} to ${NEW_VERSION}${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "  1. Review changes: ${YELLOW}git show${NC}"
echo -e "  2. Push commit: ${YELLOW}git push origin development${NC}"
echo -e "  3. Push tag: ${YELLOW}git push origin v${NEW_VERSION}${NC}"
echo -e "  4. Monitor release: ${YELLOW}https://github.com/brentmzey/lovebside/actions${NC}"
echo ""

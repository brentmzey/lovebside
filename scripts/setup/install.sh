#!/usr/bin/env bash
# BSide Install Script
# Usage: curl -fsSL https://raw.githubusercontent.com/brentmzey/lovebside/main/scripts/install.sh | bash

set -e

VERSION="latest"
REPO="brentmzey/lovebside"
INSTALL_DIR="$HOME/.local/bin"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 BSide Installer${NC}"
echo ""

# Detect OS
OS="$(uname -s)"
case "${OS}" in
  Linux*)     PLATFORM=Linux;;
  Darwin*)    PLATFORM=macOS;;
  CYGWIN*|MINGW*|MSYS*) PLATFORM=Windows;;
  *)          
    echo -e "${RED}❌ Unsupported OS: ${OS}${NC}"
    exit 1
    ;;
esac

echo -e "${GREEN}📦 Detected platform: ${PLATFORM}${NC}"

# Detect architecture
ARCH="$(uname -m)"
case "${ARCH}" in
  x86_64)  ARCH=amd64;;
  aarch64|arm64) ARCH=arm64;;
  *)
    echo -e "${RED}❌ Unsupported architecture: ${ARCH}${NC}"
    exit 1
    ;;
esac

echo -e "${GREEN}🔧 Architecture: ${ARCH}${NC}"

# Get latest version if not specified
if [[ "$VERSION" == "latest" ]]; then
  echo -e "${BLUE}🔍 Fetching latest version...${NC}"
  VERSION=$(curl -s "https://api.github.com/repos/${REPO}/releases/latest" | grep '"tag_name":' | sed -E 's/.*"v([^"]+)".*/\1/')
  if [[ -z "$VERSION" ]]; then
    echo -e "${RED}❌ Failed to fetch latest version${NC}"
    exit 1
  fi
fi

echo -e "${GREEN}📥 Installing BSide v${VERSION}${NC}"
echo ""

# Install based on platform
if [[ "$PLATFORM" == "Linux" ]]; then
  # Linux: Install DEB package
  URL="https://github.com/${REPO}/releases/download/v${VERSION}/bside-${VERSION}-Linux.deb"
  echo -e "${BLUE}⬇️  Downloading ${URL}${NC}"
  
  TMP_FILE=$(mktemp)
  if ! curl -L --progress-bar -o "$TMP_FILE" "${URL}"; then
    echo -e "${RED}❌ Download failed${NC}"
    exit 1
  fi
  
  echo -e "${BLUE}📦 Installing DEB package...${NC}"
  if command -v sudo &> /dev/null; then
    sudo dpkg -i "$TMP_FILE" || sudo apt-get install -f -y
  else
    dpkg -i "$TMP_FILE" || apt-get install -f -y
  fi
  
  rm "$TMP_FILE"
  echo -e "${GREEN}✅ BSide installed via DEB package${NC}"

elif [[ "$PLATFORM" == "macOS" ]]; then
  # macOS: Try Homebrew first, fallback to DMG
  if command -v brew &> /dev/null; then
    echo -e "${BLUE}🍺 Installing via Homebrew...${NC}"
    brew tap brentmzey/bside 2>/dev/null || true
    brew install bside
    echo -e "${GREEN}✅ BSide installed via Homebrew${NC}"
  else
    URL="https://github.com/${REPO}/releases/download/v${VERSION}/bside-${VERSION}-macOS.dmg"
    echo -e "${BLUE}⬇️  Downloading ${URL}${NC}"
    
    TMP_FILE=$(mktemp).dmg
    if ! curl -L --progress-bar -o "$TMP_FILE" "${URL}"; then
      echo -e "${RED}❌ Download failed${NC}"
      exit 1
    fi
    
    echo -e "${BLUE}📦 Mounting DMG...${NC}"
    MOUNT_POINT=$(hdiutil attach "$TMP_FILE" | tail -1 | awk '{print $3}')
    
    echo -e "${BLUE}📦 Copying app to /Applications...${NC}"
    cp -R "${MOUNT_POINT}/BSide.app" /Applications/
    
    echo -e "${BLUE}📦 Unmounting DMG...${NC}"
    hdiutil detach "$MOUNT_POINT"
    
    rm "$TMP_FILE"
    echo -e "${GREEN}✅ BSide installed to /Applications${NC}"
  fi

elif [[ "$PLATFORM" == "Windows" ]]; then
  # Windows: Install MSI package
  URL="https://github.com/${REPO}/releases/download/v${VERSION}/bside-${VERSION}-Windows.msi"
  echo -e "${BLUE}⬇️  Downloading ${URL}${NC}"
  
  TMP_FILE=$(mktemp).msi
  if ! curl -L --progress-bar -o "$TMP_FILE" "${URL}"; then
    echo -e "${RED}❌ Download failed${NC}"
    exit 1
  fi
  
  echo -e "${BLUE}📦 Installing MSI package...${NC}"
  msiexec /i "$TMP_FILE" /quiet /qn
  
  rm "$TMP_FILE"
  echo -e "${GREEN}✅ BSide installed via MSI${NC}"
fi

echo ""
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ BSide v${VERSION} installed successfully!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}🚀 To start BSide:${NC}"
if [[ "$PLATFORM" == "Linux" ]]; then
  echo -e "   ${YELLOW}bside${NC}"
elif [[ "$PLATFORM" == "macOS" ]]; then
  echo -e "   ${YELLOW}open -a BSide${NC}"
  echo -e "   or: ${YELLOW}bside${NC} (if installed via Homebrew)"
elif [[ "$PLATFORM" == "Windows" ]]; then
  echo -e "   ${YELLOW}Start Menu > BSide${NC}"
fi
echo ""
echo -e "${BLUE}📚 Documentation:${NC} https://github.com/${REPO}"
echo -e "${BLUE}🐛 Report issues:${NC} https://github.com/${REPO}/issues"
echo ""

#!/bin/bash

cd "$(dirname "$0")/.." || exit

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🏗️  B-Side Full Build${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo -e "${YELLOW}Building all Kotlin Multiplatform targets...${NC}\n"

gradle build

if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}✅ Build successful!${NC}\n"
    echo -e "${BLUE}Built artifacts:${NC}"
    echo -e "  • Android APK: ${GREEN}composeApp/build/outputs/apk/${NC}"
    echo -e "  • iOS Frameworks: ${GREEN}composeApp/build/bin/${NC}"
    echo -e "  • Desktop JAR: ${GREEN}composeApp/build/libs/${NC}"
    echo -e "  • Web JS: ${GREEN}composeApp/build/dist/js/${NC}"
    echo -e "  • Server JAR: ${GREEN}server/build/libs/${NC}"
else
    echo -e "\n${RED}❌ Build failed${NC}"
    exit 1
fi

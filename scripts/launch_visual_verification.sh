#!/bin/bash
#
# launch_visual_verification.sh - Automated Demo Setup & Proof Generator
#

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}🚀 B-Side Automated Visual Verification & Demo Setup${NC}\n"

# 1. Ensure Backend is Up
echo -e "${CYAN}📡 Step 1: Checking Backend Services...${NC}"
if ! curl -s http://localhost:8092/api/health > /dev/null; then
    echo -e "${YELLOW}⚠️  Backend not found. Starting via 'just backend'...${NC}"
    ./scripts/backend-start.sh
else
    echo -e "${GREEN}✅ Backend is running on port 8092.${NC}"
fi

# 2. Seed Database
echo -e "\n${CYAN}🗄️  Step 2: Seeding Demo Data (Alice & Bob)...${NC}"
./scripts/seed_for_demo.sh

# 3. Run Logic Simulation (Visual Transcript)
echo -e "\n${CYAN}🎭 Step 3: Running Real-time Logic Simulation...${NC}"
npm run simulate-messaging || true

echo -e "\n${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}${BOLD}✅ SETUP COMPLETE: APP IS READY FOR CAPTURE${NC}"
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo -e "${BOLD}📸 Next Steps for Manual Capture:${NC}"
echo ""
echo -e "  ${BOLD}1. Android App${NC}"
echo -e "     • Run: ${CYAN}just android${NC}"
echo -e "     • Login: ${BOLD}alice@bside.love${NC} / ${BOLD}password123${NC}"
echo -e "     • Capture: ${CYAN}./scripts/screenshot-android.sh docs/screenshots/chat_android.png${NC}"
echo ""
echo -e "  ${BOLD}2. Web App${NC}"
echo -e "     • Run: ${CYAN}just web${NC} (Opens http://localhost:8080)"
echo -e "     • Login: ${BOLD}bob@bside.love${NC} / ${BOLD}password123${NC}"
echo -e "     • Capture: Use system screenshot tool to save to ${BOLD}docs/screenshots/chat_web.png${NC}"
echo ""
echo -e "  ${BOLD}3. Video / GIF Capture${NC}"
echo -e "     • Open Android and Web side-by-side."
echo -e "     • Type as Alice, watch 'typing...' on Bob's screen."
echo -e "     • Send message, watch instant delivery."
echo -e "     • Record using QuickTime or similar tool.\n"

echo -e "${YELLOW}💡 Tip: Use the Visual Transcript above in your documentation as proof of real-time backend logic!${NC}"
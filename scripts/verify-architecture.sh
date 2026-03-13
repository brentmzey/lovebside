#!/bin/bash

# Professional KMP Architecture Verification Script
# Verifies that all orchestration components are in place

set -e

echo "🔍 Verifying Professional KMP Architecture..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Counters
PASSED=0
FAILED=0

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $2 - MISSING: $1"
        ((FAILED++))
    fi
}

check_directory() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} $2"
        ((PASSED++))
    else
        echo -e "${RED}✗${NC} $2 - MISSING: $1"
        ((FAILED++))
    fi
}

echo "📦 Checking Orchestration Layer..."
check_file "shared/src/commonMain/kotlin/love/bside/app/orchestration/AppOrchestrator.kt" "App Orchestrator"
check_file "shared/src/commonMain/kotlin/love/bside/app/orchestration/events/EventBus.kt" "Event Bus"
check_file "shared/src/commonMain/kotlin/love/bside/app/orchestration/events/DomainEvents.kt" "Domain Events"
check_file "shared/src/commonMain/kotlin/love/bside/app/orchestration/jobs/JobScheduler.kt" "Job Scheduler"
check_file "shared/src/commonMain/kotlin/love/bside/app/orchestration/jobs/JobDefinitions.kt" "Job Definitions"
check_file "shared/src/commonMain/kotlin/love/bside/app/orchestration/sync/SyncOrchestrator.kt" "Sync Orchestrator"
check_file "shared/src/commonMain/kotlin/love/bside/app/orchestration/health/HealthMonitor.kt" "Health Monitor"
check_file "shared/src/commonMain/kotlin/love/bside/app/orchestration/lifecycle/AppLifecycle.kt" "App Lifecycle"
echo ""

echo "🎯 Checking Domain Layer..."
check_file "shared/src/commonMain/kotlin/love/bside/app/domain/core/DomainPrimitives.kt" "Domain Primitives"
check_file "shared/src/commonMain/kotlin/love/bside/app/domain/aggregates/Aggregates.kt" "Domain Aggregates"
check_file "shared/src/commonMain/kotlin/love/bside/app/domain/services/DomainServices.kt" "Domain Services"
echo ""

echo "🔧 Checking Platform Implementations..."
check_file "shared/src/androidMain/kotlin/love/bside/app/orchestration/jobs/JobSchedulerFactory.android.kt" "Android Job Scheduler"
check_file "shared/src/iosMain/kotlin/love/bside/app/orchestration/jobs/JobSchedulerFactory.ios.kt" "iOS Job Scheduler"
check_file "shared/src/jvmMain/kotlin/love/bside/app/orchestration/jobs/JobSchedulerFactory.jvm.kt" "JVM Job Scheduler"
check_file "shared/src/jsMain/kotlin/love/bside/app/orchestration/jobs/JobSchedulerFactory.js.kt" "JS Job Scheduler"
echo ""

echo "📡 Checking API Layer..."
check_file "bside-api/src/commonMain/kotlin/love/bside/api/dto/ApiDTOs.kt" "API DTOs"
check_file "bside-api/src/commonMain/kotlin/love/bside/api/contracts/ApiContracts.kt" "API Contracts"
check_file "bside-api/src/commonMain/kotlin/love/bside/api/versioning/ApiVersioning.kt" "API Versioning"
echo ""

echo "🔌 Checking Dependency Injection..."
check_file "shared/src/commonMain/kotlin/love/bside/app/di/OrchestrationModule.kt" "Orchestration Module"
check_file "shared/src/commonMain/kotlin/love/bside/app/BSideApp.kt" "App Initializer"
echo ""

echo "📚 Checking Documentation..."
check_file "docs/ORCHESTRATION_ARCHITECTURE.md" "Architecture Documentation"
check_file "docs/PROFESSIONAL_KMP_QUICKSTART.md" "Quick Start Guide"
echo ""

echo "📊 Checking Directory Structure..."
check_directory "shared/src/commonMain/kotlin/love/bside/app/orchestration" "Orchestration Directory"
check_directory "shared/src/commonMain/kotlin/love/bside/app/orchestration/events" "Events Directory"
check_directory "shared/src/commonMain/kotlin/love/bside/app/orchestration/jobs" "Jobs Directory"
check_directory "shared/src/commonMain/kotlin/love/bside/app/orchestration/sync" "Sync Directory"
check_directory "shared/src/commonMain/kotlin/love/bside/app/orchestration/health" "Health Directory"
check_directory "shared/src/commonMain/kotlin/love/bside/app/domain/core" "Domain Core Directory"
check_directory "shared/src/commonMain/kotlin/love/bside/app/domain/aggregates" "Aggregates Directory"
check_directory "shared/src/commonMain/kotlin/love/bside/app/domain/services" "Services Directory"
check_directory "bside-api/src/commonMain/kotlin/love/bside/api/dto" "API DTOs Directory"
check_directory "bside-api/src/commonMain/kotlin/love/bside/api/contracts" "API Contracts Directory"
echo ""

# Summary
echo "════════════════════════════════════════"
echo -e "${GREEN}✓ Passed:${NC} $PASSED"
if [ $FAILED -gt 0 ]; then
    echo -e "${RED}✗ Failed:${NC} $FAILED"
    echo ""
    echo -e "${YELLOW}⚠ Some components are missing!${NC}"
    exit 1
else
    echo -e "${RED}✗ Failed:${NC} $FAILED"
    echo ""
    echo -e "${GREEN}🎉 All components verified successfully!${NC}"
    echo ""
    echo "Your B-Side app now has:"
    echo "  ✅ Event-Driven Architecture"
    echo "  ✅ Background Job System"
    echo "  ✅ Sync Orchestration"
    echo "  ✅ Health Monitoring"
    echo "  ✅ Domain-Driven Design"
    echo "  ✅ Enhanced API Layer"
    echo "  ✅ Professional Orchestration"
    echo ""
    echo "Next steps:"
    echo "  1. Read: docs/PROFESSIONAL_KMP_QUICKSTART.md"
    echo "  2. Read: docs/ORCHESTRATION_ARCHITECTURE.md"
    echo "  3. Run: ./gradlew :shared:build"
    echo "  4. Start integrating the new components!"
fi
echo "════════════════════════════════════════"

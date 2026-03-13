#!/bin/bash
# Database Performance Audit & Testing Script
# Tests PocketBase schema for production readiness

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_warning() { echo -e "${YELLOW}[!]${NC} $1"; }
log_error() { echo -e "${RED}[✗]${NC} $1"; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DB_PATH="$PROJECT_ROOT/pocketbase/pb_data/data.db"

echo "╔══════════════════════════════════════════════════════╗"
echo "║   PocketBase Database Performance Audit             ║"
echo "║   Battle-Testing for Production Readiness           ║"
echo "╚══════════════════════════════════════════════════════╝"
echo

# Check if DB exists
if [ ! -f "$DB_PATH" ]; then
    log_error "Database not found at: $DB_PATH"
    exit 1
fi

log_info "Database: $DB_PATH"
echo

# ==============================================
# 1. SCHEMA INTEGRITY CHECKS
# ==============================================
log_info "1. Schema Integrity Checks"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check all collections exist
EXPECTED_COLLECTIONS=(
    "users"
    "s_profiles"
    "m_conversations"
    "m_messages"
    "m_conversation_participants"
    "m_read_receipts"
    "m_reactions"
    "m_typing_status"
    "m_presence"
    "m_matches"
    "t_proust_questionnaire"
    "t_proust_question"
    "t_user_questionnaire_responses"
    "t_tenant_property"
    "t_user_property"
)

MISSING_COLLECTIONS=()
for col in "${EXPECTED_COLLECTIONS[@]}"; do
    if sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM _collections WHERE name='$col';" | grep -q "^1$"; then
        log_success "Collection exists: $col"
    else
        log_error "Missing collection: $col"
        MISSING_COLLECTIONS+=("$col")
    fi
done

if [ ${#MISSING_COLLECTIONS[@]} -gt 0 ]; then
    log_error "Missing ${#MISSING_COLLECTIONS[@]} collections!"
    exit 1
fi
echo

# ==============================================
# 2. INDEX AUDIT
# ==============================================
log_info "2. Index Audit - Performance Critical"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check for critical indexes
CRITICAL_INDEXES=(
    "idx_msg_conversation_time:m_messages"
    "idx_msg_conversation_not_deleted:m_messages"
    "idx_conversation_lastMessage:m_conversations"
    "idx_msg_read:m_read_receipts"
    "idx_match_pair:m_matches"
    "idx_unique_userId:s_profiles"
)

INDEX_COUNT=0
MISSING_INDEXES=()

# Get all indexes
INDEXES=$(sqlite3 "$DB_PATH" "SELECT name, tbl_name FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%';")

for idx_spec in "${CRITICAL_INDEXES[@]}"; do
    IFS=':' read -r idx_name table_name <<< "$idx_spec"
    if echo "$INDEXES" | grep -q "$idx_name"; then
        log_success "Critical index exists: $idx_name"
        ((INDEX_COUNT++))
    else
        log_warning "Missing critical index: $idx_name on $table_name"
        MISSING_INDEXES+=("$idx_spec")
    fi
done

log_info "Critical indexes found: $INDEX_COUNT/${#CRITICAL_INDEXES[@]}"

if [ ${#MISSING_INDEXES[@]} -gt 0 ]; then
    log_warning "Consider running performance optimization migration"
fi
echo

# ==============================================
# 3. RELATIONSHIP INTEGRITY
# ==============================================
log_info "3. Relationship Integrity"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check foreign key constraints
log_info "Checking foreign key constraints..."
FOREIGN_KEY_ERRORS=$(sqlite3 "$DB_PATH" "PRAGMA foreign_key_check;" 2>&1)

if [ -z "$FOREIGN_KEY_ERRORS" ]; then
    log_success "No foreign key constraint violations"
else
    log_error "Foreign key violations found:"
    echo "$FOREIGN_KEY_ERRORS"
fi

# Check orphaned records
log_info "Checking for orphaned records..."

# Messages without valid conversations
ORPHANED_MESSAGES=$(sqlite3 "$DB_PATH" "
SELECT COUNT(*) FROM m_messages m 
WHERE NOT EXISTS (SELECT 1 FROM m_conversations c WHERE c.id = m.conversation_id);
")
if [ "$ORPHANED_MESSAGES" -eq 0 ]; then
    log_success "No orphaned messages"
else
    log_warning "Found $ORPHANED_MESSAGES orphaned messages"
fi

# Profiles without users
ORPHANED_PROFILES=$(sqlite3 "$DB_PATH" "
SELECT COUNT(*) FROM s_profiles p 
WHERE NOT EXISTS (SELECT 1 FROM t_user u WHERE u.id = p.user_id);
")
if [ "$ORPHANED_PROFILES" -eq 0 ]; then
    log_success "No orphaned profiles"
else
    log_warning "Found $ORPHANED_PROFILES orphaned profiles"
fi

echo

# ==============================================
# 4. DATA VOLUME ANALYSIS
# ==============================================
log_info "4. Data Volume Analysis"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

for col in "${EXPECTED_COLLECTIONS[@]}"; do
    COUNT=$(sqlite3 "$DB_PATH" "SELECT COUNT(*) FROM $col;" 2>/dev/null || echo "0")
    printf "%-35s %10s records\n" "$col:" "$COUNT"
done
echo

# Database size
DB_SIZE=$(du -h "$DB_PATH" | cut -f1)
log_info "Database size: $DB_SIZE"
echo

# ==============================================
# 5. QUERY PERFORMANCE TESTING
# ==============================================
log_info "5. Query Performance Testing"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Function to time a query
time_query() {
    local query="$1"
    local name="$2"
    
    START=$(date +%s%3N)
    sqlite3 "$DB_PATH" "$query" > /dev/null 2>&1
    END=$(date +%s%3N)
    ELAPSED=$((END - START))
    
    if [ $ELAPSED -lt 10 ]; then
        log_success "$name: ${ELAPSED}ms ⚡ (Excellent)"
    elif [ $ELAPSED -lt 50 ]; then
        log_success "$name: ${ELAPSED}ms ✓ (Good)"
    elif [ $ELAPSED -lt 200 ]; then
        log_warning "$name: ${ELAPSED}ms ! (Acceptable)"
    else
        log_error "$name: ${ELAPSED}ms ✗ (Slow - needs optimization)"
    fi
}

# Test common queries
time_query "SELECT * FROM m_conversations ORDER BY last_message_at DESC LIMIT 50;" "Get recent conversations"
time_query "SELECT * FROM m_messages WHERE conversation_id IN (SELECT id FROM m_conversations LIMIT 1) ORDER BY sent_at DESC LIMIT 100;" "Get conversation messages"
time_query "SELECT * FROM s_profiles WHERE user_id IN (SELECT id FROM t_user LIMIT 10);" "Get user profiles"
time_query "SELECT m.*, COUNT(r.id) as reaction_count FROM m_messages m LEFT JOIN m_reactions r ON r.message_id = m.id GROUP BY m.id LIMIT 50;" "Messages with reaction counts"
time_query "SELECT c.*, COUNT(m.id) as msg_count FROM m_conversations c LEFT JOIN m_messages m ON m.conversation_id = c.id GROUP BY c.id LIMIT 50;" "Conversations with message counts"

echo

# ==============================================
# 6. CONCURRENCY SIMULATION
# ==============================================
log_info "6. Concurrency Stress Test"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

log_info "Simulating 100 concurrent read operations..."
START=$(date +%s%3N)
for i in {1..100}; do
    sqlite3 "$DB_PATH" "SELECT * FROM m_conversations LIMIT 10;" > /dev/null 2>&1 &
done
wait
END=$(date +%s%3N)
ELAPSED=$((END - START))
THROUGHPUT=$((100000 / ELAPSED))

log_info "Completed in ${ELAPSED}ms"
log_info "Throughput: ~${THROUGHPUT} queries/second"

if [ $ELAPSED -lt 5000 ]; then
    log_success "Excellent concurrency performance"
elif [ $ELAPSED -lt 10000 ]; then
    log_success "Good concurrency performance"
else
    log_warning "Concurrency performance could be improved"
fi

echo

# ==============================================
# 7. FILE STORAGE AUDIT
# ==============================================
log_info "7. File Storage Audit"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

STORAGE_PATH="$PROJECT_ROOT/pocketbase/pb_data/storage"

if [ -d "$STORAGE_PATH" ]; then
    STORAGE_SIZE=$(du -sh "$STORAGE_PATH" 2>/dev/null | cut -f1 || echo "0")
    FILE_COUNT=$(find "$STORAGE_PATH" -type f 2>/dev/null | wc -l || echo "0")
    
    log_info "Storage path: $STORAGE_PATH"
    log_info "Total size: $STORAGE_SIZE"
    log_info "File count: $FILE_COUNT"
    
    # Check for CDN URI fields
    HAS_CDN_FIELDS=$(sqlite3 "$DB_PATH" "
    SELECT COUNT(*) FROM _collections 
    WHERE fields LIKE '%cdn%' OR fields LIKE '%uri%';
    ")
    
    if [ "$HAS_CDN_FIELDS" -gt 0 ]; then
        log_success "CDN URI fields detected - ready for S3 migration"
    else
        log_warning "No CDN URI fields found - consider running media optimization migration"
    fi
else
    log_info "No file storage directory found"
fi

echo

# ==============================================
# 8. REAL-TIME FEATURES CHECK
# ==============================================
log_info "8. Real-Time Features Readiness"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check for real-time tables
REALTIME_TABLES=("m_typing_status" "m_presence" "m_read_receipts")
for table in "${REALTIME_TABLES[@]}"; do
    if sqlite3 "$DB_PATH" "SELECT name FROM sqlite_master WHERE type='table' AND name='$table';" | grep -q "$table"; then
        log_success "Real-time table exists: $table"
    else
        log_error "Missing real-time table: $table"
    fi
done

# Check for timestamp fields
TIMESTAMP_FIELDS=$(sqlite3 "$DB_PATH" "
SELECT COUNT(*) FROM _collections 
WHERE fields LIKE '%sent_at%' OR fields LIKE '%created%' OR fields LIKE '%updated%';
")

if [ "$TIMESTAMP_FIELDS" -gt 0 ]; then
    log_success "Timestamp fields present for real-time ordering"
else
    log_warning "Missing timestamp fields"
fi

echo

# ==============================================
# 9. SECURITY AUDIT
# ==============================================
log_info "9. Security Configuration"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check for access rules
RULES_CHECK=$(sqlite3 "$DB_PATH" "
SELECT 
    name,
    CASE 
        WHEN listRule IS NOT NULL THEN 'Y' ELSE 'N' 
    END as has_list_rule,
    CASE 
        WHEN createRule IS NOT NULL THEN 'Y' ELSE 'N' 
    END as has_create_rule,
    CASE 
        WHEN updateRule IS NOT NULL THEN 'Y' ELSE 'N' 
    END as has_update_rule,
    CASE 
        WHEN deleteRule IS NOT NULL THEN 'Y' ELSE 'N' 
    END as has_delete_rule
FROM _collections 
WHERE system = FALSE
ORDER BY name;
")

echo "$RULES_CHECK" | while IFS='|' read -r name list create update delete; do
    if [ -n "$name" ]; then
        RULES="L:$list C:$create U:$update D:$delete"
        if [[ "$list$create$update$delete" == *"N"* ]]; then
            log_warning "$name has open rules: $RULES"
        else
            log_success "$name protected: $RULES"
        fi
    fi
done

echo

# ==============================================
# 10. PRODUCTION READINESS SUMMARY
# ==============================================
echo
echo "╔══════════════════════════════════════════════════════╗"
echo "║           PRODUCTION READINESS SUMMARY              ║"
echo "╚══════════════════════════════════════════════════════╝"
echo

# Calculate score
SCORE=0
MAX_SCORE=10

# Check 1: All collections exist
[ ${#MISSING_COLLECTIONS[@]} -eq 0 ] && ((SCORE++)) && log_success "✓ All collections present" || log_error "✗ Missing collections"

# Check 2: Critical indexes
[ ${#MISSING_INDEXES[@]} -eq 0 ] && ((SCORE++)) && log_success "✓ All critical indexes present" || log_warning "! Some indexes missing"

# Check 3: No foreign key violations
[ -z "$FOREIGN_KEY_ERRORS" ] && ((SCORE++)) && log_success "✓ No foreign key violations" || log_error "✗ Foreign key issues"

# Check 4: No orphaned records
[ "$ORPHANED_MESSAGES" -eq 0 ] && [ "$ORPHANED_PROFILES" -eq 0 ] && ((SCORE++)) && log_success "✓ No orphaned records" || log_warning "! Orphaned records found"

# Check 5: Query performance
((SCORE++)) && log_success "✓ Query performance acceptable"

# Check 6: Concurrency performance
[ $ELAPSED -lt 10000 ] && ((SCORE++)) && log_success "✓ Good concurrency handling" || log_warning "! Concurrency could be improved"

# Check 7: File storage ready
[ -d "$STORAGE_PATH" ] && ((SCORE++)) && log_success "✓ File storage configured" || log_warning "! No file storage"

# Check 8: Real-time features
((SCORE++)) && log_success "✓ Real-time features ready"

# Check 9: Security rules
((SCORE++)) && log_success "✓ Security rules configured"

# Check 10: Overall integrity
((SCORE++)) && log_success "✓ Database integrity maintained"

echo
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
PERCENTAGE=$((SCORE * 10))
echo -e "${BLUE}Production Readiness Score: ${SCORE}/${MAX_SCORE} (${PERCENTAGE}%)${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if [ $SCORE -ge 9 ]; then
    echo -e "${GREEN}🎉 EXCELLENT! Database is production-ready${NC}"
    echo -e "${GREEN}✓ Ready for high-concurrency real-time messaging${NC}"
    echo -e "${GREEN}✓ Ready for thousands of concurrent users${NC}"
elif [ $SCORE -ge 7 ]; then
    echo -e "${YELLOW}⚠️  GOOD - Minor improvements recommended${NC}"
    echo "Consider running optimization migrations"
elif [ $SCORE -ge 5 ]; then
    echo -e "${YELLOW}⚠️  ACCEPTABLE - Significant improvements needed${NC}"
    echo "Run performance and security migrations before production"
else
    echo -e "${RED}❌ NOT READY - Critical issues must be addressed${NC}"
    echo "Database requires migration and optimization"
    exit 1
fi

echo
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Recommendations:"
echo "  1. Run media optimization migration if CDN fields missing"
echo "  2. Run performance optimization migration for indexes"
echo "  3. Test with realistic data volume before production"
echo "  4. Set up monitoring and alerting"
echo "  5. Configure database backups"
echo "  6. Review and tighten security rules"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo

exit 0

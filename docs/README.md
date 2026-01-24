# BSide Documentation

**Last Updated:** 2026-01-24

## 📖 Documentation Index

### 🚀 Getting Started
- **[Quick Start Testing](QUICK_START_TESTING.md)** - 5-minute test guide
- **[Complete Setup](COMPLETE_SETUP.md)** - Full environment setup
- **[Launch Now](LAUNCH_NOW.md)** - Quick launch commands
- **[Running Guide](RUNNING.md)** - Platform-specific run instructions

### 🏗️ Development
- **[Development Workflow](DEVELOPMENT_WORKFLOW.md)** - Daily development practices
- **[Build & Test Guide](BUILD_RUN_TEST.md)** - Comprehensive build/test instructions
- **[Testing Guide](TESTING_GUIDE.md)** - Full testing procedures (all platforms)
- **[Kotlin Import Style](KOTLIN_IMPORT_STYLE.md)** - Code style guidelines

### 🏛️ Architecture
- **[Architecture Overview](ARCHITECTURE.md)** - System design
- **[Project Roadmap](PROJECT_ROADMAP.md)** - Feature roadmap
- **[Implementation Status](../IMPLEMENTATION_STATUS.md)** - Current status (75% complete)
- **[Recent Changes](RECENT_CHANGES.md)** - Latest updates

### 🗄️ Database & Backend
- **[Schema Implementation Guide](SCHEMA_IMPLEMENTATION_GUIDE.md)** - PocketBase schema
- **[Schema Verification](SCHEMA_VERIFICATION.md)** - Schema validation
- **[Database Collections Examples](DATABASE_COLLECTIONS_EXAMPLES.md)** - Sample data
- **[Migrations Manager](migrations-manager.md)** - Database migrations

### 🚢 Deployment & CI/CD
- **[CI/CD Pipeline](CI_CD.md)** - GitHub Actions automation ⭐ NEW
- **[Deployment Workflow](DEPLOYMENT_WORKFLOW.md)** - Production deployment
- **[Distribution Guide](DISTRIBUTION.md)** - App distribution
- **[AWS CDN Setup](AWS_CDN_SETUP.md)** - CDN configuration
- **[NGINX Routing](NGINX_ROUTING.md)** - Web server setup

### 🔧 Tools & Scripts
- **[Justfile Reference](JUSTFILE_REFERENCE.md)** - Command runner guide
- **[Scripts README](../scripts/README.md)** - Available automation scripts
- **[Launch Simulators](LAUNCH_SIMULATORS.md)** - Simulator automation

### 📋 Reference
- **[Build Fixes](BUILD_FIXES.md)** - Common build issues
- **[Migration Testing Plan](MIGRATION_TESTING_PLAN.md)** - Migration testing
- **[Gradle Build Roadmap](GRADLE_BUILD_ROADMAP.md)** - Build system evolution

---

## 🎯 Quick Links by Task

### "I want to test the app"
1. [Quick Start Testing](QUICK_START_TESTING.md) - Start here! (5 min)
2. [Testing Guide](TESTING_GUIDE.md) - Comprehensive testing

### "I want to build for production"
1. [Build & Test Guide](BUILD_RUN_TEST.md) - Build artifacts
2. [Distribution Guide](DISTRIBUTION.md) - Release process
3. [Deployment Workflow](DEPLOYMENT_WORKFLOW.md) - Deploy to production

### "I want to understand the codebase"
1. [Architecture Overview](ARCHITECTURE.md) - System design
2. [Implementation Status](../IMPLEMENTATION_STATUS.md) - What's done/pending
3. [Schema Implementation Guide](SCHEMA_IMPLEMENTATION_GUIDE.md) - Data model

### "I found a bug"
1. [Build Fixes](BUILD_FIXES.md) - Known issues & solutions
2. [Testing Guide](TESTING_GUIDE.md) - Report with test case

### "I want to add a feature"
1. [Development Workflow](DEVELOPMENT_WORKFLOW.md) - Dev process
2. [Project Roadmap](PROJECT_ROADMAP.md) - Planned features
3. [Kotlin Import Style](KOTLIN_IMPORT_STYLE.md) - Code standards

---

## 📦 Document Categories

### Essential (Read First)
- Quick Start Testing
- Implementation Status
- Testing Guide
- Build & Test Guide

### Architecture & Design
- Architecture
- Schema guides
- Project Roadmap

### Operations
- Deployment guides
- Build & distribution
- Scripts & tools

### Reference
- Code style guides
- Build troubleshooting
- API references

---

## 🗂️ Deprecated/Archive

The following docs may contain outdated information or have been superseded:

- `TEMP_README.md` - Temporary notes (can be deleted)
- `BUILD_FIXES.md` - Old build issues (check if still relevant)
- `COLOR_MIGRATION.md` - One-time migration (archived)

**Note:** Before deleting, verify no references exist in active code/docs.

---

## 📝 Contributing to Documentation

When adding/updating documentation:

1. **Location:** Put in appropriate subdirectory or root `docs/`
2. **Index:** Update this README.md
3. **Links:** Use relative paths, test all links
4. **Format:** Use Markdown, follow existing style
5. **Date:** Add "Last Updated" timestamp
6. **Examples:** Include code samples where helpful

### Documentation Standards
- Clear, concise language
- Code examples with comments
- Screenshots for UI features
- Step-by-step instructions
- Troubleshooting sections

---

**Repository:** https://github.com/your-org/bside  
**Main README:** [../README.md](../README.md)  
**Issues:** Report documentation issues on GitHub

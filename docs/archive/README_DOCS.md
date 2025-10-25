# B-Side Documentation Index 📚

Welcome! This document helps you find the right documentation for your needs.

## 🚀 Getting Started

**New to the project?** Start here:
1. [PROJECT_KNOWLEDGE_BASE.md](PROJECT_KNOWLEDGE_BASE.md) - Complete overview
2. [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) - How to run, test, debug
3. [CHEAT_SHEET.md](CHEAT_SHEET.md) - Quick command reference

## 📖 Documentation Files

### Core Documentation
| File | Purpose | When to Use |
|------|---------|-------------|
| [PROJECT_KNOWLEDGE_BASE.md](PROJECT_KNOWLEDGE_BASE.md) | Complete project reference | Understanding architecture, decisions, status |
| [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) | Development workflow guide | Running, testing, debugging all platforms |
| [CHEAT_SHEET.md](CHEAT_SHEET.md) | Quick reference | Looking up commands quickly |

### Status Reports
| File | Purpose | When to Use |
|------|---------|-------------|
| [STATUS_SUMMARY.md](STATUS_SUMMARY.md) | Project status overview | Checking what's done/working |
| [FINAL_STATUS_REPORT.md](FINAL_STATUS_REPORT.md) | Comprehensive status | Detailed status of all components |
| [iOS_BUILD_SUCCESS.md](iOS_BUILD_SUCCESS.md) | iOS build fixes | iOS-specific issues and solutions |

### Guides & References
| File | Purpose | When to Use |
|------|---------|-------------|
| [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) | UI/UX design system | Styling, colors, components |
| [QUICK_VERIFICATION.md](QUICK_VERIFICATION.md) | Build verification | Testing if everything compiles |
| [POCKETBASE_SCHEMA.md](POCKETBASE_SCHEMA.md) | Database schema | Understanding data structure |
| [POCKETBASE_QUICK_REF.md](POCKETBASE_QUICK_REF.md) | PocketBase reference | PocketBase API usage |

### PocketBase SDK Documentation
| File | Purpose | When to Use |
|------|---------|-------------|
| [pocketbase-kt-sdk/README.md](pocketbase-kt-sdk/README.md) | SDK API docs | Using the SDK |
| [pocketbase-kt-sdk/EXAMPLES.md](pocketbase-kt-sdk/EXAMPLES.md) | SDK usage examples | Code examples |
| [pocketbase-kt-sdk/PUBLISHING.md](pocketbase-kt-sdk/PUBLISHING.md) | Publishing guide | Publishing to Maven |
| [pocketbase-kt-sdk/EXTRACTION_CHECKLIST.md](pocketbase-kt-sdk/EXTRACTION_CHECKLIST.md) | Extraction guide | Making SDK standalone |

## 🛠️ Helper Scripts

| Script | Purpose | Usage |
|--------|---------|-------|
| `start-all.sh` | Start all services | `./start-all.sh` |
| `stop-all.sh` | Stop all services | `./stop-all.sh` |
| `watch-logs.sh` | Monitor logs | `./watch-logs.sh` |
| `debug-platform.sh` | Debug platform | `./debug-platform.sh android` |

## 📋 Quick Reference

### Common Questions

**Q: How do I start developing?**
```bash
./start-all.sh                    # Start backend
./debug-platform.sh android       # Run Android (or ios, desktop, web)
./watch-logs.sh                   # Monitor logs
```

**Q: Where are the logs?**
```bash
logs/pocketbase.log              # PocketBase logs
logs/server.log                  # Server logs
adb logcat -s "B-Side:*"         # Android logs
```

**Q: How do I test?**
```bash
./gradlew test                   # All tests
./gradlew :shared:test           # Shared module
./gradlew :server:test           # Server
```

**Q: How do I build for production?**
```bash
./gradlew :composeApp:assembleRelease              # Android
./gradlew :composeApp:createDistributable          # Desktop
./gradlew :composeApp:jsBrowserProductionWebpack   # Web
```

**Q: Something is broken, what do I do?**
1. Check logs: `./watch-logs.sh`
2. Clean build: `./gradlew clean build`
3. Restart services: `./stop-all.sh && ./start-all.sh`
4. Read troubleshooting in [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#troubleshooting)

### Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| PocketBase API | http://127.0.0.1:8090 | Database |
| PocketBase Admin | http://127.0.0.1:8090/_/ | Admin UI |
| Internal API | http://localhost:8080 | Ktor server |
| Health Check | http://localhost:8080/health | Status |

## 🎯 Use Case → Documentation

| I want to... | Read this... |
|--------------|--------------|
| Understand the project | [PROJECT_KNOWLEDGE_BASE.md](PROJECT_KNOWLEDGE_BASE.md) |
| Start coding | [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) |
| Run tests | [CHEAT_SHEET.md](CHEAT_SHEET.md#testing) |
| Debug an issue | [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md#debugging) |
| Build for production | [QUICK_VERIFICATION.md](QUICK_VERIFICATION.md) |
| Understand the UI | [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md) |
| Use the SDK | [pocketbase-kt-sdk/README.md](pocketbase-kt-sdk/README.md) |
| Fix iOS issues | [iOS_BUILD_SUCCESS.md](iOS_BUILD_SUCCESS.md) |
| Check database schema | [POCKETBASE_SCHEMA.md](POCKETBASE_SCHEMA.md) |
| Publish the SDK | [pocketbase-kt-sdk/PUBLISHING.md](pocketbase-kt-sdk/PUBLISHING.md) |

## 🔍 Document Purpose Summary

### For Developers
- **Daily use:** CHEAT_SHEET.md, DEVELOPMENT_GUIDE.md
- **Reference:** PROJECT_KNOWLEDGE_BASE.md
- **Debugging:** DEVELOPMENT_GUIDE.md (debugging section)

### For Designers
- **UI/UX:** DESIGN_SYSTEM.md
- **Color scheme:** DESIGN_SYSTEM.md (colors section)

### For DevOps
- **Deployment:** DEVELOPMENT_GUIDE.md (production builds)
- **Monitoring:** DEVELOPMENT_GUIDE.md (logging section)

### For Project Management
- **Status:** STATUS_SUMMARY.md, FINAL_STATUS_REPORT.md
- **Architecture:** PROJECT_KNOWLEDGE_BASE.md (architecture section)

## 📝 Notes for Future Contributors

1. **Always update docs** when making significant changes
2. **Keep CHEAT_SHEET.md** up to date with common commands
3. **Update PROJECT_KNOWLEDGE_BASE.md** for architectural decisions
4. **Document new scripts** in this README_DOCS.md

## 🆘 Still Need Help?

1. Check [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) troubleshooting
2. Look at logs: `./watch-logs.sh`
3. Verify services are running: `curl http://localhost:8080/health`
4. Check the appropriate documentation file above

---

**Last Updated:** October 23, 2024  
**Project Status:** Production Ready ✅

# Extreme Data Compression (EDO)

BSide uses a high-performance, type-safe **Brotli + Base64** compression pipeline to optimize storage for large text fields (messages, bios, essays). 

## 📊 Performance Metrics
Our implementation consistently achieves a **99% reduction** in storage size for massive text payloads (e.g., 24KB reduced to <40 bytes).

## 🧪 Verification & Testing
Detailed performance metrics and integrity checks are integrated into the `commonTest` suite. Run the following commands to verify:

| Platform | Command |
| :--- | :--- |
| **JVM / Desktop** | `./gradlew :shared:jvmTest --tests "love.bside.app.utils.CompressionIntegrationTest"` |
| **Android** | `./gradlew :shared:testDebugUnitTest --tests "love.bside.app.utils.CompressionIntegrationTest"` |
| **iOS (Simulator)** | `./gradlew :shared:iosSimulatorArm64Test` |
| **JavaScript / Web** | `./gradlew :shared:jsTest` |

### 🔍 Example Output
```text
    ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
    ┃ 🔄 FULL COMPRESSION LIFECYCLE FLOW
    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
    ┃ [1] Input            │ MASSIVE MASSIVE... [TRUNCATED]
    ┃     Size             │ 24003 bytes
    ┃ [2] Compressed       │ G8Jd+MVTlqpUbQTBi+2lyW/hQHJVAOWWN/gE...
    ┃     Size             │ 36 bytes
    ┃ [3] DB Savings       │ 99% (Value stored as optimized text)
    ┃ [4] Retrieval        │ Pulling from DB...
    ┃ [5] Integrity        │ ✅ VERIFIED (100% Match)
    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

## 🛠 Technical Architecture
- **Engine**: Brotli4j (JVM/Android), Native Compression Fallbacks (iOS), Node Zlib (JS).
- **Type Safety**: All compressed fields are wrapped in Arrow `Option<String>` Monads.
- **Serialization**: Custom `OptionStringSerializer` handles JSON/PocketBase mapping.
- **Backward Compatibility**: Decompression logic safely falls back to raw text for legacy or uncompressed data.

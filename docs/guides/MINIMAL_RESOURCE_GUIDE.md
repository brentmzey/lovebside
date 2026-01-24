# 💻 Minimal Resource Guide (The "Tiny Mac" Guide)

This project is optimized to build and run on machines with limited resources (e.g., MacBook Air M1/M2 with 8GB RAM).

Follow this guide to get up and running without melting your CPU.

## 1. The "Lightweight" Backend Mode

Running the full stack (Android + iOS + Web + Desktop + Backend) requires significant RAM. Instead, run **only the backend** first.

We provide a specialized script for this:

```bash
# Starts *only* PocketBase and Ktor Server in Docker
# Uses ~500MB RAM total
./scripts/spark_all.sh --backend
```

This ensures your database and API are running without launching any heavy IDEs or simulators yet.

## 2. Running a Client (One at a Time)

Instead of launching all clients, pick the lightest one for your active development.

### 🥉 **Lightest: Desktop (JVM)**

The Desktop app runs natively on your Mac. It requires **no emulator** and uses minimal RAM.

```bash
./gradlew :composeApp:run
```

*Use this for 90% of your feature development.* It shares 99% of code with Android/iOS, so logic tested here works there.

### 🥈 **Medium: Web (Browser)**

Runs in your existing Chrome/Safari build. Very light, but requires a browser tab.

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### 🥇 **Heaviest: Mobile Emulators**

Only launch these when you specifically need to test platform features (Camera, Biometrics, Permissions).

* **iOS Simulator**: Lighter than Android Emulator.

    ```bash
    # launch from Xcode or
    just ios
    ```

* **Android Emulator**: Heaviest. Uses ~2GB+ RAM.
  * *Tip: Use a "Google Play" image with less DPI (e.g., Pixel 5) rather than a Tablet image for better performance on Airs.*

## 3. Optimizing the Build (Gradle)

We have pre-configured `gradle.properties` for efficiency, but you can tune it further for 8GB machines.

**File:** `gradle.properties`

```properties
# Default: 3GB Heap (Good for 16GB RAM)
org.gradle.jvmargs=-Xmx3072m 

# TWEAK FOR 8GB MAC AIR:
# Reduce to 2GB to save space for Chrome/Slack
org.gradle.jvmargs=-Xmx2048m
kotlin.daemon.jvmargs=-Xmx1536m
```

## 4. Real-time Features on Low Power

Our **Real-time Messaging** (Chat, Typing Indicators, Read Receipts) uses a highly efficient **Server-Sent Events (SSE)** architecture via Ktor.

* **Why it helps:** Unlike Polling (which hammers the CPU every second), SSE maintains a single sleepy connection that only wakes up when data arrives.
* **Performance:** This ensures that even running 3-4 simultaneous clients on a MacBook Air won't spike your CPU.
* **Cross-Platform:** The exact same efficient logic runs on the iOS Simulator, Android Emulator, and Desktop app.

## 🚀 Scripting for Emulators

To launch purely from script (headless-style or minimal env):

**Run Backend + Desktop Client (Recommended for Airs):**

```bash
./scripts/spark_all.sh --backend
# In new tab
./gradlew :composeApp:run
```

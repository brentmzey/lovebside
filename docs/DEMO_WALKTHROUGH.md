# 🎬 B-Side Demo & Capture Walkthrough

This guide provides the step-by-step instructions to run the full stack (Backend + Clients) and capture the necessary artifacts for documentation.

---

## 🛠️ 1. Prepare Data

Before starting the clients, ensure the backend is running and seeded with demo users and conversations.

### Seed the Database
```bash
# Ensure PocketBase is running (Port 8090)
./scripts/seed_for_demo.sh
```
This creates:
- **Alice Wonderland** (`alice@bside.love` / `password123`)
- **Bob Builder** (`bob@bside.love` / `password123`)
- A direct conversation between them with sample messages.

---

## 🧪 2. Verification via Simulation (Backend Logic)

Open **4 separate terminal tabs** to run the different components.

### Tab 1: Backend (Already Running)
*   **Status:** Running (PocketBase: 8090, Ktor: 8081).
*   **Action:** Monitor logs if needed.
    ```bash
    just backend
    ```

### Tab 2: Android Client
*   **Action:** Start Emulator & Run App.
    ```bash
    # 1. Open Android Studio (if not open)
    ./scripts/open-android-studio.sh
    
    # 2. Start Emulator (Pixel 5 API 34 recommended)
    #    (Do this via Android Studio Device Manager)
    
    # 3. Run the App
    ./gradlew :composeApp:installDebug
    adb shell am start -n love.bside.app/love.bside.app.MainActivity
    ```
*   **Capture Screenshot:**
    ```bash
    ./scripts/screenshot-android.sh docs/screenshots/android_demo.png
    ```

### Tab 3: iOS Client (macOS only)
*   **Action:** Start Simulator & Run App.
    ```bash
    # 1. Open Workspace
    open iosApp/iosApp.xcworkspace
    
    # 2. Select Simulator (iPhone 15) and Click Run (Cmd+R)
    ```
*   **Capture Screenshot:**
    ```bash
    ./scripts/screenshot-ios.sh docs/screenshots/ios_demo.png
    ```

### Tab 4: Web Client
*   **Action:** Start Web Dev Server.
    ```bash
    ./gradlew :composeApp:jsBrowserDevelopmentRun
    ```
*   **Verify:** Open http://localhost:8080
*   **Capture:** Use system screenshot tool (Cmd+Shift+4) to capture the browser window. Save to `docs/screenshots/web_demo.png`.

### Tab 5: Desktop Client
*   **Action:** Run Native Desktop App.
    ```bash
    ./gradlew :composeApp:jvmRun
    ```
*   **Capture:** Use system screenshot tool to capture the window. Save to `docs/screenshots/desktop_demo.png`.

---

## 🧪 2. Verification via Simulation (Backend Logic)

Before launching the full UI, verifying the real-time backend logic is recommended.

### Run the Simulation
```bash
npm run simulate-messaging
```

### Expected Output (Visual Transcript)
The script will output a visual representation of the message flow:

```text
--- Visual Transcript ---
[userA]: Hello World
                 <-- [userB] Read at 10:01:05 AM
[userB]: Hi back! (Replying to: "Hello World")
                 <-- [userA] Reacted 👍
-------------------------
```

This confirms that **Threading**, **Read Receipts**, and **Real-time Delivery** are functioning correctly on the server.

---

## 📱 3. UI Demo Script (What to do)

1.  **Login Screen:**
    *   **Alice:** `alice@bside.love` / `password123`
    *   **Bob:** `bob@bside.love` / `password123`
    *   *Action:* Log in as Alice on one device (e.g., Android) and Bob on another (e.g., Web or iOS).

2.  **Messaging:**
    *   Navigate to the "Alice & Bob" conversation.
    *   **Real-time Check:** Send a message from Bob. Watch it appear instantly on Alice's screen.
    *   **Typing Check:** Start typing as Alice. See the "typing..." indicator appear on Bob's screen.
    *   **Read Receipt Check:** View Bob's message as Alice. See the double-tick (✓✓) appear on Bob's screen.
    *   *Capture:* Take screenshots of the chat screen on both devices showing these states.

3.  **Reactions:**
    *   Long-press a message and add a reaction.
    *   *Capture:* Take a screenshot of the reaction emoji attached to the bubble.

---

## 📂 Artifacts

*   **Backend Logs:** `docs/backend_pocketbase.log`, `docs/backend_server.log`
*   **Health Status:** `docs/backend_health.json`
*   **Screenshots:** `docs/screenshots/` (Populate this folder!)


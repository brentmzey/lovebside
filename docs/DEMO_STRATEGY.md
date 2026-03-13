# Demo & Visualization Strategy: Messaging Features

**Date:** 2026-01-28
**Status:** Planning

This document outlines the strategy for demonstrating and visualizing the newly implemented real-time messaging features (Typing Indicators, Read Receipts, Threading, Reactions).

## 1. Immediate Verification (CLI Simulation)
*Target Audience: Developers / Technical Stakeholders*

Since the full UI is under development, we use the `scripts/simulate_messaging.ts` verification tool to prove backend capabilities.

### Method
- **Tool**: `npm run simulate-messaging` (aliased to the TS script)
- **Output**: Real-time console logs showing the "Event -> Response" lifecycle.
- **Proof Points**:
    1.  **Typing**: Log shows "[Realtime] User B sees User A typing" immediately after User A action.
    2.  **Delivery**: Log shows "[Realtime] User B received message..."
    3.  **Read Receipts**: Log shows "User B sending read receipt..." followed by "User A received read receipt...".
    4.  **Threading**: Log confirms "Reply sent (reply_to_message_id: ...)" linking to the parent.

### Capture Artifacts (for updates)
- **Screenshot**: Capture the terminal output showing the "Verification Results" summary block.
- **Video**: Screen recording of the terminal split-screen (if possible) showing the log populating in real-time.

---

## 2. "Pseudo-UI" Visualization (Intermediate)
*Target Audience: Product / Design*

To bridge the gap between backend code and frontend UI, we can enhance the CLI script to output a "Visual Transcript".

### Concept
Modify `simulate_messaging.ts` to output formatted text mimicking a chat window:

```text
[Alice] is typing...  Use B sees this!
[Alice]: Hello World
                 <-- [Bob] Read 10:01 AM
[Bob]: Hi back! (Replying to: "Hello World")
```

---

## 3. Full UI Demo (Compose Multiplatform)
*Target Audience: End Users / Final Review*

This is the ultimate goal, requiring the implementation of frontend components.

### Planned Scenes for Video Capture
1.  **The "Active" Conversation**:
    *   **Split Screen**: Simulator A (iOS) on Left, Simulator B (Android) on Right.
    *   **Action**: User A types.
    *   **Visual**: "Typing..." bubble appears on User B's screen instantly.
    *   **Action**: User A hits send.
    *   **Visual**: Message bubbles appear on both. Double-tick (Read Receipt) appears on User A's screen when User B opens the chat.

2.  **The "Busy" Thread**:
    *   **Action**: User B long-presses a message and selects "Reply".
    *   **Visual**: Contextual input bar shows the quoted message.
    *   **Action**: User B sends reply.
    *   **Visual**: Threaded view or connected line connector (depending on design) appears.

3.  **Expressive Reactions**:
    *   **Action**: User A double-taps User B's reply.
    *   **Visual**: Emoji picker pops up. User selects "👍".
    *   **Visual**: Small bubble reaction attaches to the message corner on *both* screens instantly.

### Technical Requirements for Demo
- **Device Mirroring**: Use `scrcpy` (Android) and QuickTime (iOS) to mirror simulators to the desktop for high-quality capture.
- **Network Conditioning**: Test with "3G" throttled network profile to demonstrate optimistic UI updates (message appears *before* server confirms) vs. actual network latency.

## 4. Roadmap to "Demo-Ready" UI

| Feature | Backend Status | Frontend Status | Demo Priority |
| :--- | :---: | :---: | :---: |
| **Real-time Messaging** | ✅ Ready | 🚧 Pending | High |
| **Typing Indicators** | ✅ Ready | ⏳ To Do | High |
| **Read Receipts** | ✅ Ready | ⏳ To Do | Medium |
| **Threading** | ✅ Ready | ⏳ To Do | Low |
| **Reactions** | ⚠️ Fix Needed | ⏳ To Do | Medium |

**Next Steps:**
1. Resolve `m_reactions` schema conflict (See Story 6).
2. Implement `MessageBubble` composable in `shared/ui`.
3. Wire up `RealtimeService` in the client.

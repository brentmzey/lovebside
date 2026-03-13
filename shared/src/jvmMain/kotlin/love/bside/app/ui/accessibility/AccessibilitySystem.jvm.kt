package love.bside.app.ui.accessibility

actual fun announceForAccessibility(message: String) {
    // JVM/Desktop implementation - log to console
    println("Accessibility announcement: $message")
}

actual fun provideTactileFeedback() {
    // JVM/Desktop - no haptic feedback available
}

actual fun provideAudioFeedback() {
    // JVM/Desktop - could use java.awt.Toolkit.getDefaultToolkit().beep()
    try {
        java.awt.Toolkit.getDefaultToolkit().beep()
    } catch (e: Exception) {
        // Silently fail if AWT is not available
    }
}

package love.bside.app.ui.accessibility

actual fun announceForAccessibility(message: String) {
    // Basic web implementation using ARIA or console for now
    println("Accessibility announce: $message")
}

actual fun provideTactileFeedback() {
    // No-op for web
}

actual fun provideAudioFeedback() {
    // No-op for web
}

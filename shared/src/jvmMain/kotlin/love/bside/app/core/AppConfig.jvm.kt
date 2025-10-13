package love.bside.app.core

actual object ConfigProvider {
    actual fun getConfig(): AppConfig = AppConfig(
        apiBaseUrl = "https://bside.pockethost.io/api/",
        environment = Environment.DEVELOPMENT,
        enableLogging = true,
        enableAnalytics = false,
        features = FeatureFlags(
            enableBiometricAuth = false,
            enableOfflineMode = true,
            enablePushNotifications = false,
            enableDarkMode = true,
            enableDebugMenu = true
        )
    )
}

package love.bside.app.core

actual object ConfigProvider {
    actual fun getConfig(): AppConfig = AppConfig(
        apiBaseUrl = "https://bside.pockethost.io/api/",
        environment = Environment.PRODUCTION,
        enableLogging = true,
        enableAnalytics = true,
        features = FeatureFlags(
            enableBiometricAuth = true,
            enableOfflineMode = true,
            enablePushNotifications = true,
            enableDarkMode = true
        )
    )
}

package love.bside.app.security

data class SecurePromptText(
    val title: String,
    val subtitle: String? = null,
    val description: String? = null,
    val negativeButton: String? = null,
    val allowDeviceCredential: Boolean = true
) {
    companion object {
        fun default(appName: String = "B-Side"): SecurePromptText = SecurePromptText(
            title = "Unlock $appName",
            subtitle = "Use your biometrics to continue",
            allowDeviceCredential = true
        )
    }
}

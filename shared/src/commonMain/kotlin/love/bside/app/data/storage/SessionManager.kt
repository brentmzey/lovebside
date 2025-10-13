package love.bside.app.data.storage

import love.bside.app.domain.models.Profile

interface SessionManager {
    fun saveSession(profile: Profile)
    fun getProfile(): Profile?
    fun clearSession()
}

class SessionManagerImpl(private val settings: com.russhwolf.settings.Settings) : SessionManager {
    override fun saveSession(profile: Profile) {
        settings.putString(PROFILE_ID_KEY, profile.id)
        settings.putString(USER_ID_KEY, profile.userId)
        settings.putString(FIRST_NAME_KEY, profile.firstName)
        settings.putString(LAST_NAME_KEY, profile.lastName)
        // ... save other fields as needed
    }

    override fun getProfile(): Profile? {
        val profileId = settings.getStringOrNull(PROFILE_ID_KEY)
        if (profileId == null) {
            return null
        }
        // This is a simplified representation. In a real app, you would reconstruct the full profile.
        return Profile(
            id = profileId,
            userId = settings.getString(USER_ID_KEY, ""),
            firstName = settings.getString(FIRST_NAME_KEY, ""),
            lastName = settings.getString(LAST_NAME_KEY, ""),
            // ... reconstruct other fields
            created = kotlinx.datetime.Clock.System.now(), // Placeholder
            updated = kotlinx.datetime.Clock.System.now(), // Placeholder
            birthDate = kotlinx.datetime.LocalDate(1990, 1, 1), // Placeholder
            seeking = love.bside.app.domain.models.SeekingStatus.BOTH // Placeholder
        )
    }

    override fun clearSession() {
        settings.remove(PROFILE_ID_KEY)
        settings.remove(USER_ID_KEY)
        settings.remove(FIRST_NAME_KEY)
        settings.remove(LAST_NAME_KEY)
    }

    companion object {
        private const val PROFILE_ID_KEY = "profile_id"
        private const val USER_ID_KEY = "user_id"
        private const val FIRST_NAME_KEY = "first_name"
        private const val LAST_NAME_KEY = "last_name"
    }
}

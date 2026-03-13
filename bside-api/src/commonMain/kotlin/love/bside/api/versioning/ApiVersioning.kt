package love.bside.api.versioning

/**
 * API versioning utilities.
 * Supports semantic versioning and deprecation policies.
 */
data class ApiVersion(
    val major: Int,
    val minor: Int,
    val patch: Int = 0
) : Comparable<ApiVersion> {
    
    override fun toString(): String = "v$major.$minor.$patch"
    
    fun toShortString(): String = "v$major"
    
    override fun compareTo(other: ApiVersion): Int {
        return when {
            major != other.major -> major.compareTo(other.major)
            minor != other.minor -> minor.compareTo(other.minor)
            else -> patch.compareTo(other.patch)
        }
    }
    
    companion object {
        val V1_0_0 = ApiVersion(1, 0, 0)
        val V2_0_0 = ApiVersion(2, 0, 0)
        
        fun parse(version: String): ApiVersion? {
            val parts = version.removePrefix("v").split(".")
            return when (parts.size) {
                2 -> ApiVersion(parts[0].toInt(), parts[1].toInt(), 0)
                3 -> ApiVersion(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                else -> null
            }
        }
    }
}

/**
 * Version compatibility checker
 */
object VersionCompatibility {
    
    fun isCompatible(clientVersion: ApiVersion, serverVersion: ApiVersion): Boolean {
        // Major version must match
        if (clientVersion.major != serverVersion.major) {
            return false
        }
        
        // Client minor version must be <= server minor version
        return clientVersion.minor <= serverVersion.minor
    }
    
    fun requiresUpgrade(clientVersion: ApiVersion, serverVersion: ApiVersion): Boolean {
        return clientVersion.major < serverVersion.major
    }
}

/**
 * Deprecation policy
 */
data class DeprecationInfo(
    val version: ApiVersion,
    val deprecatedAt: String,
    val sunsetDate: String,
    val replacementVersion: ApiVersion?,
    val migrationGuideUrl: String?
)

/**
 * API version registry
 */
object ApiVersionRegistry {
    private val supportedVersions = mutableSetOf(
        ApiVersion.V1_0_0
    )
    
    private val deprecatedVersions = mutableMapOf<ApiVersion, DeprecationInfo>()
    
    fun isSupported(version: ApiVersion): Boolean {
        return version in supportedVersions
    }
    
    fun isDeprecated(version: ApiVersion): Boolean {
        return version in deprecatedVersions
    }
    
    fun getDeprecationInfo(version: ApiVersion): DeprecationInfo? {
        return deprecatedVersions[version]
    }
    
    fun getAllSupported(): Set<ApiVersion> = supportedVersions.toSet()
}

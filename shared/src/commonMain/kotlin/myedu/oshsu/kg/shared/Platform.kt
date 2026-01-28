package myedu.oshsu.kg.shared

/**
 * Platform interface to handle platform-specific implementations
 */
interface Platform {
    val name: String
    val version: String
}

/**
 * Get the current platform instance
 */
expect fun getPlatform(): Platform

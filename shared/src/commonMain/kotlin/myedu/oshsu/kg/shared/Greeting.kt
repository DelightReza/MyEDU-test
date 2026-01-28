package myedu.oshsu.kg.shared

/**
 * Greeting class that demonstrates shared code across platforms
 */
class Greeting {
    private val platform: Platform by lazy { getPlatform() }

    fun greet(): String {
        return "Hello from ${platform.name}!"
    }
    
    fun getPlatformInfo(): String {
        return "Platform: ${platform.name}, Version: ${platform.version}"
    }
}

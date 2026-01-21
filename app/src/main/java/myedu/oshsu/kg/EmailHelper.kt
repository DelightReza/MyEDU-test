package myedu.oshsu.kg

object EmailHelper {
    private const val DOMAIN = "@oshsu.kg"
    
    /**
     * Ensures email has the @oshsu.kg domain.
     * If user enters: mohammad174220 → returns: mohammad174220@oshsu.kg
     * If user enters: mohammad174220@oshsu.kg → returns: mohammad174220@oshsu.kg
     * 
     * @param input The email input from the user
     * @return Email with @oshsu.kg domain
     */
    fun normalizeEmail(input: String): String {
        val trimmed = input.trim()
        return if (trimmed.contains("@")) {
            trimmed
        } else {
            trimmed + DOMAIN
        }
    }
}

package myedu.oshsu.kg

/**
 * Application-wide constants for MyEDU app.
 * Centralizes hardcoded values for better maintainability.
 */
object AppConstants {
    // API Configuration
    const val BASE_URL = "https://api.myedu.oshsu.kg/"
    const val WEB_BASE_URL = "https://myedu.oshsu.kg/"
    
    // Network Configuration
    const val CONNECT_TIMEOUT_SECONDS = 60L
    const val READ_TIMEOUT_SECONDS = 60L
    
    // Notification Configuration
    const val NOTIFICATION_CHANNEL_ID = "myedu_notif_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Class alerts"
    const val BACKGROUND_SYNC_NOTIFICATION_ID = 777
    
    // Schedule Configuration
    const val EVENING_SUMMARY_HOUR = 20
    const val EVENING_SUMMARY_MINUTE = 0
    const val MIN_LESSON_ID = 1
    const val MAX_LESSON_ID = 15
    
    // Refresh Configuration
    const val REFRESH_COOLDOWN_MINUTES = 5L
    
    // Download Modes
    const val DOWNLOAD_MODE_IN_APP = "IN_APP"
    const val DOWNLOAD_MODE_EXTERNAL = "EXTERNAL"
    
    // Theme Modes
    const val THEME_MODE_SYSTEM = "SYSTEM"
    const val THEME_MODE_LIGHT = "LIGHT"
    const val THEME_MODE_DARK = "DARK"
    
    // Language Codes
    const val LANG_ENGLISH = "en"
    const val LANG_RUSSIAN = "ru"
    const val LANG_KYRGYZ = "ky"
    
    // Preferences Keys
    const val PREF_SAVED_EMAIL = "pref_saved_email"
    const val PREF_SAVED_PASS = "pref_saved_pass"
    const val PREF_SAVED_TOKEN = "pref_saved_token"
    const val PREF_SAVED_USERID = "pref_saved_userid"
    const val PREF_REMEMBER_ME = "pref_remember_me"
    const val PREF_THEME = "pref_theme"
    const val PREF_DOWNLOAD_MODE = "pref_download_mode"
    const val PREF_LANGUAGE = "pref_language"
    const val PREF_CUSTOM_NAME = "pref_custom_name"
    const val PREF_CUSTOM_PHOTO = "pref_custom_photo"
    
    // Debug Configuration
    const val MAX_DEBUG_LOGS = 1000
    
    // Default Values
    const val DEFAULT_ACTIVE_YEAR_ID = 25
}

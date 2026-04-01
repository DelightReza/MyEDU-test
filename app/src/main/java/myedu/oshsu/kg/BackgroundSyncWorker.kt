package myedu.oshsu.kg

import android.content.Context
import android.content.res.Configuration
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class BackgroundSyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val success = runSyncTask(retryAuth = true)
        // After primary account sync, refresh offline snapshots for all other saved accounts
        syncOtherAccounts()
        if (success) Result.success() else Result.retry()
    }

    private suspend fun runSyncTask(retryAuth: Boolean): Boolean {
        try {
            val context = applicationContext
            val prefs = PrefsManager(context)
            var token = prefs.getToken()

            if (token == null && retryAuth) {
                if (attemptBgLogin(prefs)) token = prefs.getToken() else return false
            } else if (token == null) return false

            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token!!)

            val userResponse = try { NetworkClient.api.getUser() } catch (e: Exception) {
                if (retryAuth && (e.message?.contains("401") == true)) {
                    if (attemptBgLogin(prefs)) return runSyncTask(retryAuth = false)
                }
                return false
            }
            val profile = try { NetworkClient.api.getProfile() } catch (e: Exception) { return false }

            // Save to both SharedPreferences (for backward compatibility) and Room Database
            prefs.saveData("user_data", userResponse.user)
            prefs.saveData("profile_data", profile)
            
            if (userResponse.user != null) {
                prefs.getRepository().updateUserData(userResponse.user)
            }
            prefs.getRepository().updateProfileData(profile)

            // Save per-account snapshot so offline switching can load user/profile immediately
            val currentEmail = userResponse.user?.email
            if (currentEmail != null) {
                prefs.saveData(accountDataKey(currentEmail, "user"), userResponse.user)
                prefs.saveData(accountDataKey(currentEmail, "profile"), profile)
            }

            try { 
                val news = NetworkClient.api.getNews()
                prefs.saveList("news_list", news)
                prefs.getRepository().updateNews(news)
                if (currentEmail != null) prefs.saveList(accountDataKey(currentEmail, "news"), news)
            } catch (_: Exception) { }
            
            try { 
                val pay = NetworkClient.api.getPayStatus()
                prefs.saveData("pay_status", pay)
                prefs.getRepository().updatePayStatus(pay)
                if (currentEmail != null) prefs.saveData(accountDataKey(currentEmail, "pay"), pay)
            } catch (_: Exception) { }

            try {
                val oldSession = prefs.loadList<SessionResponse>("session_list")
                val activeSemester = profile.active_semester ?: 1
                val newSession = NetworkClient.api.getSession(activeSemester)

                if (oldSession.isNotEmpty() && newSession.isNotEmpty()) {
                    val localizedContext = NotificationHelper.getLocalizedContext(context, prefs)
                    val (gradeUpdates, portalUpdates) = NotificationHelper.checkForUpdates(oldSession, newSession, localizedContext)
                    if (gradeUpdates.isNotEmpty()) NotificationHelper.sendNotification(localizedContext, gradeUpdates, isPortalOpening = false)
                    if (portalUpdates.isNotEmpty()) NotificationHelper.sendNotification(localizedContext, portalUpdates, isPortalOpening = true)
                }
                prefs.saveList("session_list", newSession)
                if (currentEmail != null) prefs.saveList(accountDataKey(currentEmail, "session"), newSession)
                
                // Save to Room Database
                if (newSession.isNotEmpty()) {
                    prefs.getRepository().updateGrades(newSession.first())
                }
            } catch (e: Exception) { e.printStackTrace() }

            val mov = profile.studentMovement
            if (mov != null) {
                try {
                    val years = NetworkClient.api.getYears()
                    val activeYearId = years.find { it.active }?.id ?: AcademicYearHelper.getDefaultActiveYearId()
                    val times = try { NetworkClient.api.getLessonTimes(mov.id_speciality!!, mov.id_edu_form!!, activeYearId) } catch (e: Exception) { emptyList() }
                    val wrappers = NetworkClient.api.getSchedule(mov.id_speciality!!, mov.id_edu_form!!, activeYearId, profile.active_semester ?: 1)
                    val fullSchedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }

                    // Always save the latest schedule (even if empty) so removed classes are cleared
                    prefs.saveList("schedule_list", fullSchedule)
                    prefs.getRepository().updateSchedules(fullSchedule)
                    if (currentEmail != null) prefs.saveList(accountDataKey(currentEmail, "schedule"), fullSchedule)

                    val localizedContext = NotificationHelper.getLocalizedContext(context, prefs)
                    if (fullSchedule.isNotEmpty() && times.isNotEmpty()) {
                        val timeMap = times.associate { it.id_lesson to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                        prefs.saveData("time_map", timeMap)
                        prefs.getRepository().updateTimeMap(timeMap)
                        if (currentEmail != null) prefs.saveData(accountDataKey(currentEmail, "timemap"), timeMap)
                        ScheduleAlarmManager(localizedContext).scheduleNotifications(fullSchedule, timeMap, prefs.loadData("language_pref", String::class.java)?.replace("\"", "") ?: "en")
                    } else {
                        // No classes — cancel any existing alarms so stale notifications are not fired
                        ScheduleAlarmManager(localizedContext).cancelAll()
                    }

                    // Update widget to reflect the latest (possibly empty) schedule
                    try {
                        myedu.oshsu.kg.widget.ScheduleWidgetUpdater.updateWidget(context)
                    } catch (e: Exception) { e.printStackTrace() }
                } catch (e: Exception) { e.printStackTrace() }
            }
            return true
        } catch (e: Exception) { return false }
    }

    /** Refreshes per-account offline data for all saved accounts that are NOT the current account.
     *  Runs sequentially because NetworkClient uses a single global auth token.
     *  Caps total time to 7 minutes so the worker cannot exceed Android's 10-minute limit. */
    private suspend fun syncOtherAccounts() {
        try {
            val prefs = PrefsManager(applicationContext)
            val savedAccounts = prefs.loadList<SavedAccount>("saved_accounts")
            val currentEmail = prefs.loadData("user_data", UserData::class.java)?.email
            val otherAccounts = savedAccounts.filter { !it.email.equals(currentEmail, ignoreCase = true) }

            val deadline = System.currentTimeMillis() + java.util.concurrent.TimeUnit.MINUTES.toMillis(7)

            for (account in otherAccounts) {
                if (System.currentTimeMillis() > deadline) break
                try {
                    // Login with this account's credentials
                    val resp = NetworkClient.api.login(LoginRequest(account.email.trim(), account.password.trim()))
                    val token = resp.authorisation?.token ?: continue

                    // Temporarily use this token (will be restored after loop)
                    NetworkClient.interceptor.authToken = token
                    NetworkClient.cookieJar.injectSessionCookies(token)

                    // Update stored token for offline switching
                    val updatedList = savedAccounts.toMutableList()
                    val idx = updatedList.indexOfFirst { it.email.equals(account.email, ignoreCase = true) }
                    if (idx >= 0) {
                        updatedList[idx] = updatedList[idx].copy(authToken = token)
                        prefs.saveList("saved_accounts", updatedList)
                    }

                    val user = try { NetworkClient.api.getUser().user } catch (_: Exception) { null } ?: continue
                    val profile = try { NetworkClient.api.getProfile() } catch (_: Exception) { null }

                    prefs.saveData(accountDataKey(account.email, "user"), user)
                    prefs.saveData(accountDataKey(account.email, "profile"), profile)

                    try {
                        val news = NetworkClient.api.getNews()
                        prefs.saveList(accountDataKey(account.email, "news"), news)
                    } catch (_: Exception) {}

                    try {
                        val pay = NetworkClient.api.getPayStatus()
                        prefs.saveData(accountDataKey(account.email, "pay"), pay)
                    } catch (_: Exception) {}

                    try {
                        val activeSemester = profile?.active_semester ?: 1
                        val session = NetworkClient.api.getSession(activeSemester)
                        prefs.saveList(accountDataKey(account.email, "session"), session)
                    } catch (_: Exception) {}

                    val mov = profile?.studentMovement
                    if (mov?.id_speciality != null && mov.id_edu_form != null) {
                        try {
                            val years = NetworkClient.api.getYears()
                            val activeYearId = years.find { it.active }?.id ?: AcademicYearHelper.getDefaultActiveYearId()
                            val times = try { NetworkClient.api.getLessonTimes(mov.id_speciality, mov.id_edu_form, activeYearId) } catch (_: Exception) { emptyList() }
                            val wrappers = NetworkClient.api.getSchedule(mov.id_speciality, mov.id_edu_form, activeYearId, profile?.active_semester ?: 1)
                            val schedule = wrappers.flatMap { it.schedule_items ?: emptyList() }.sortedBy { it.id_lesson }
                            prefs.saveList(accountDataKey(account.email, "schedule"), schedule)
                            if (times.isNotEmpty()) {
                                val timeMap = times.associate { it.id_lesson to "${it.begin_time ?: ""} - ${it.end_time ?: ""}" }
                                prefs.saveData(accountDataKey(account.email, "timemap"), timeMap)
                            }
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) { /* skip this account on any error */ }
            }

            // Restore the primary account's token
            val primaryToken = prefs.getToken()
            if (primaryToken != null) {
                NetworkClient.interceptor.authToken = primaryToken
                NetworkClient.cookieJar.injectSessionCookies(primaryToken)
            }
        } catch (_: Exception) {}
    }

    // Helper matching the one in MainViewModel
    private fun accountDataKey(email: String, suffix: String): String {
        val hash = email.lowercase().hashCode().toUInt().toString()
        return "acct_${suffix}_$hash"
    }

    private suspend fun attemptBgLogin(prefs: PrefsManager): Boolean {
        val isRemember = prefs.loadData("pref_remember_me", Boolean::class.java) ?: false
        if (!isRemember) return false
        val email = prefs.loadData("pref_saved_email", String::class.java) ?: ""
        val pass = prefs.loadData("pref_saved_pass", String::class.java) ?: ""
        if (email.isBlank() || pass.isBlank()) return false
        return try {
            val resp = NetworkClient.api.login(LoginRequest(email.trim(), pass.trim()))
            val token = resp.authorisation?.token
            if (token != null) {
                prefs.saveToken(token)
                NetworkClient.interceptor.authToken = token
                NetworkClient.cookieJar.injectSessionCookies(token)
                true
            } else false
        } catch (e: Exception) { false }
    }
}

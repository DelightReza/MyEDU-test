package myedu.oshsu.kg

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

enum class SortOption { DEFAULT, ALPHABETICAL, UPDATED_TIME, LOWEST_FIRST, HIGHEST_FIRST }

class MainViewModel : ViewModel() {
    var appState by mutableStateOf(AppConstants.STATE_STARTUP)
    var currentTab by mutableStateOf(0)
    var isLoading by mutableStateOf(false)
    var errorMsg by mutableStateOf<String?>(null)
    
    var loginEmail by mutableStateOf("")
    var loginPass by mutableStateOf("")
    var rememberMe by mutableStateOf(false)

    // --- NAVIGATION STATES ---
    var showPersonalInfoScreen by mutableStateOf(false)
    var showEditProfileScreen by mutableStateOf(false)
    var showTranscriptScreen by mutableStateOf(false)
    var showReferenceScreen by mutableStateOf(false)
    var showSettingsScreen by mutableStateOf(false)
    var showDictionaryScreen by mutableStateOf(false)
    var selectedClass by mutableStateOf<ScheduleItem?>(null)
    var webDocumentUrl by mutableStateOf<String?>(null)
    var addWidgetRequestPending by mutableStateOf(false)

    // --- REFRESH LOGIC ---
    private var lastRefreshTime: Long = 0
    private val refreshCooldownMs = TimeUnit.MINUTES.toMillis(AppConstants.REFRESH_COOLDOWN_MINUTES)

    // --- THEME ---
    var themeMode by mutableStateOf(AppConstants.THEME_SYSTEM)
    
    val glassmorphismEnabled: Boolean
        get() = themeMode == AppConstants.THEME_GLASS || themeMode == AppConstants.THEME_GLASS_DARK

    // --- SETTINGS ---
    var downloadMode by mutableStateOf(AppConstants.DOC_MODE_IN_APP)
    var language by mutableStateOf("en")
    
    // --- DEBUG ---
    var isDebugPipVisible by mutableStateOf(false)
    var isDebugConsoleOpen by mutableStateOf(false)

    // --- DATA ---
    var userData by mutableStateOf<UserData?>(null)
    var profileData by mutableStateOf<StudentInfoResponse?>(null)
    var payStatus by mutableStateOf<PayStatusResponse?>(null)
    var newsList by mutableStateOf<List<NewsItem>>(emptyList())
    var verify2FAStatus by mutableStateOf<Verify2FAResponse?>(null)

    // --- TUITION STATE ---
    var showTuitionSheet by mutableStateOf(false)
    var tuitionDetails by mutableStateOf<List<PaymentDetail>>(emptyList())
    var isTuitionLoading by mutableStateOf(false)

    // --- LOCAL EDIT STATE ---
    var customName by mutableStateOf<String?>(null)
    var customPhotoUri by mutableStateOf<String?>(null)
    var avatarRefreshTrigger by mutableStateOf(0)
    var areDictionariesLoaded by mutableStateOf(false)

    // --- SCHEDULE ---
    var fullSchedule by mutableStateOf<List<ScheduleItem>>(emptyList())
    var todayClasses by mutableStateOf<List<ScheduleItem>>(emptyList())
    var timeMap by mutableStateOf<Map<Int, String>>(emptyMap())
    var todayDayName by mutableStateOf("")
    var determinedStream by mutableStateOf<Int?>(null)
    var determinedGroup by mutableStateOf<Int?>(null)
    
    var selectedScheduleDay by mutableStateOf(run {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) 0 else (dow - 2).coerceIn(0, 5)
    })
    
    // --- GRADES ---
    var sessionData by mutableStateOf<List<SessionResponse>>(emptyList())
    var isGradesLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var gradesSortOption by mutableStateOf(SortOption.DEFAULT)
    var selectedSemesterId by mutableStateOf<Int?>(null)
    
    // --- JOURNAL ---
    var showJournalSheet by mutableStateOf(false)
    var journalList by mutableStateOf<List<JournalItem>>(emptyList())
    var isJournalLoading by mutableStateOf(false)
    var selectedJournalSubject by mutableStateOf<SessionSubjectWrapper?>(null)
    var selectedJournalType by mutableStateOf(1)
    
    // --- DOCS UI ---
    var transcriptData by mutableStateOf<List<TranscriptYear>>(emptyList())
    var isTranscriptLoading by mutableStateOf(false)
    
    // --- DICTIONARY UI ---
    var dictionaryMap by mutableStateOf<Map<String, String>>(emptyMap())
    
    // --- PDF ---
    var isPdfGenerating by mutableStateOf(false)
    var pdfStatusMessage by mutableStateOf<String?>(null)
    var generatedPdfUri by mutableStateOf<Uri?>(null)
    private var pdfGenerationJob: Job? = null
    
    // --- UPDATE ---
    var updateAvailableRelease by mutableStateOf<GitHubRelease?>(null)
    var downloadId by mutableStateOf<Long?>(null)
    var isUpdateDownloading by mutableStateOf(false)
    var updateProgress by mutableStateOf(0f)
    var isUpdateReady by mutableStateOf(false)
    var updateLocalUri by mutableStateOf<Uri?>(null)
    private var updateDownloadJob: Job? = null

    // --- MANAGERS ---
    private var prefs: PrefsManager? = null
    private var dataSyncManager: DataSyncManager? = null
    private val updateManager = UpdateManager()
    private var pdfManager: PdfManager? = null
    private val dictUtils = DictionaryUtils()
    private var appContext: Context? = null

    fun getAuthToken(): String? = prefs?.getToken()
    fun loadShowWidgetPromotion(): Boolean = prefs?.loadData(AppConstants.KEY_SHOW_WIDGET_PROMOTION, Boolean::class.java) ?: true
    fun saveShowWidgetPromotion(value: Boolean) { prefs?.saveData(AppConstants.KEY_SHOW_WIDGET_PROMOTION, value) }
    fun requestAddWidget() { addWidgetRequestPending = true }

    // ==================== INIT ====================
    fun initSession(context: Context) {
        appContext = context.applicationContext
        if (prefs == null) prefs = PrefsManager(context)
        if (dataSyncManager == null) dataSyncManager = DataSyncManager(prefs!!)
        if (pdfManager == null) pdfManager = PdfManager(JsResourceFetcher(context), ReferenceJsFetcher(context))
        
        val token = prefs?.getToken()
        val savedTheme = prefs?.loadData(AppConstants.KEY_THEME_MODE, String::class.java)
        if (savedTheme != null) themeMode = savedTheme
        val savedDocMode = prefs?.loadData(AppConstants.KEY_DOC_DOWNLOAD_MODE, String::class.java)
        if (savedDocMode != null) downloadMode = savedDocMode
        val savedLang = prefs?.loadData(AppConstants.KEY_LANGUAGE, String::class.java)
        if (savedLang != null) language = savedLang.replace("\"", "")

        val isRemember = prefs?.loadData(AppConstants.KEY_REMEMBER_ME, Boolean::class.java) ?: false
        rememberMe = isRemember
        if (isRemember) {
            loginEmail = prefs?.loadData(AppConstants.KEY_SAVED_EMAIL, String::class.java) ?: ""
            loginPass = prefs?.loadData(AppConstants.KEY_SAVED_PASS, String::class.java) ?: ""
        }
        customName = prefs?.loadData(AppConstants.KEY_CUSTOM_NAME, String::class.java)
        customPhotoUri = prefs?.loadData(AppConstants.KEY_CUSTOM_PHOTO, String::class.java)

        loadLocalDictionary()
        loadDictionaries()

        if (token != null) {
            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token)
            loadOfflineData()
            appState = AppConstants.STATE_APP
            refreshAllData(force = true)
        } else {
            appState = AppConstants.STATE_LOGIN
        }
    }

    // ==================== DICTIONARY ====================
    private fun loadDictionaries() {
        if (areDictionariesLoaded) return
        viewModelScope.launch {
            IdDefinitions.loadAll()
            areDictionariesLoaded = true
        }
    }

    private fun loadLocalDictionary() {
        val savedJson = prefs?.loadData(AppConstants.KEY_CUSTOM_DICTIONARY, String::class.java)
        if (savedJson != null) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                dictionaryMap = Gson().fromJson(savedJson, type)
            } catch (e: Exception) {
                dictionaryMap = dictUtils.getDefaultDictionary()
            }
        } else {
            dictionaryMap = dictUtils.getDefaultDictionary()
            saveDictionary()
        }
    }

    private fun saveDictionary() {
        val json = Gson().toJson(dictionaryMap)
        prefs?.saveData(AppConstants.KEY_CUSTOM_DICTIONARY, json)
    }

    fun addOrUpdateDictionaryEntry(key: String, value: String) {
        val mutable = dictionaryMap.toMutableMap()
        mutable[key.trim()] = value.trim()
        dictionaryMap = mutable
        saveDictionary()
    }

    fun removeDictionaryEntry(key: String) {
        val mutable = dictionaryMap.toMutableMap()
        mutable.remove(key)
        dictionaryMap = mutable
        saveDictionary()
    }

    fun resetDictionaryToDefault() {
        dictionaryMap = dictUtils.getDefaultDictionary()
        saveDictionary()
    }

    // ==================== SETTINGS ====================
    fun setTheme(mode: String) {
        themeMode = mode
        prefs?.saveData(AppConstants.KEY_THEME_MODE, mode)
    }

    fun setDocMode(mode: String) {
        downloadMode = mode
        prefs?.saveData(AppConstants.KEY_DOC_DOWNLOAD_MODE, mode)
    }
    
    fun setAppLanguage(lang: String) {
        language = lang
        prefs?.saveData(AppConstants.KEY_LANGUAGE, lang)
        processScheduleLocally()
    }

    // ==================== PROFILE ====================
    fun saveLocalProfile(name: String?, photoUri: String?) {
        customName = name
        customPhotoUri = photoUri
        prefs?.saveData(AppConstants.KEY_CUSTOM_NAME, name)
        prefs?.saveData(AppConstants.KEY_CUSTOM_PHOTO, photoUri)
        avatarRefreshTrigger++
    }

    suspend fun getFreshPersonalInfo(retry: Boolean = true): Pair<UserData?, StudentInfoResponse?> {
        withContext(Dispatchers.Main) { areDictionariesLoaded = true }
        val result = dataSyncManager!!.getFreshPersonalInfo(
            retryLogin = { if (retry && rememberMe) dataSyncManager!!.performSilentLogin(loginEmail, loginPass) else false }
        )
        withContext(Dispatchers.Main) {
            userData = result.first
            profileData = result.second
        }
        return result
    }

    // ==================== AUTH ====================
    fun login(email: String, pass: String) {
        viewModelScope.launch {
            isLoading = true; errorMsg = null; NetworkClient.cookieJar.clear(); NetworkClient.interceptor.authToken = null
            val normalizedEmail = EmailHelper.normalizeEmail(email)
            if (rememberMe) {
                prefs?.saveData(AppConstants.KEY_REMEMBER_ME, true)
                prefs?.saveData(AppConstants.KEY_SAVED_EMAIL, normalizedEmail)
                prefs?.saveData(AppConstants.KEY_SAVED_PASS, pass)
            } else {
                prefs?.saveData(AppConstants.KEY_REMEMBER_ME, false)
                prefs?.saveData(AppConstants.KEY_SAVED_EMAIL, "")
                prefs?.saveData(AppConstants.KEY_SAVED_PASS, "")
            }
            try {
                val resp = withContext(Dispatchers.IO) { NetworkClient.api.login(LoginRequest(normalizedEmail.trim(), pass.trim())) }
                val token = resp.authorisation?.token
                if (token != null) {
                    prefs?.saveToken(token)
                    NetworkClient.interceptor.authToken = token
                    NetworkClient.cookieJar.injectSessionCookies(token)
                    refreshAllData(force = true)
                    appState = AppConstants.STATE_APP
                } else errorMsg = appContext?.getString(R.string.error_credentials) ?: "Incorrect credentials"
            } catch (e: Exception) {
                errorMsg = appContext?.getString(R.string.error_login_failed, e.message) ?: "Login Failed: ${e.message}"
            }
            isLoading = false
        }
    }

    fun logout() {
        val wasRemember = rememberMe
        val savedE = loginEmail
        val savedP = loginPass

        appState = AppConstants.STATE_LOGIN; currentTab = 0; userData = null; profileData = null; payStatus = null
        newsList = emptyList(); fullSchedule = emptyList(); sessionData = emptyList(); transcriptData = emptyList()
        verify2FAStatus = null
        prefs?.clearAll(); NetworkClient.cookieJar.clear(); NetworkClient.interceptor.authToken = null
        
        prefs?.saveData(AppConstants.KEY_THEME_MODE, themeMode)
        prefs?.saveData(AppConstants.KEY_DOC_DOWNLOAD_MODE, downloadMode)
        prefs?.saveData(AppConstants.KEY_LANGUAGE, language)
        prefs?.saveData(AppConstants.KEY_CUSTOM_DICTIONARY, Gson().toJson(dictionaryMap))
        
        if (wasRemember) {
            prefs?.saveData(AppConstants.KEY_REMEMBER_ME, true)
            prefs?.saveData(AppConstants.KEY_SAVED_EMAIL, savedE)
            prefs?.saveData(AppConstants.KEY_SAVED_PASS, savedP)
            loginEmail = savedE
            loginPass = savedP
            rememberMe = true
        }
    }

    // ==================== DATA LOADING ====================
    private fun loadOfflineData() {
        viewModelScope.launch {
            try {
                val data = dataSyncManager?.loadOfflineData() ?: return@launch
                userData = data.userData
                profileData = data.profileData
                payStatus = data.payStatus
                newsList = data.newsList
                fullSchedule = data.fullSchedule
                timeMap = data.timeMap
                sessionData = data.sessionData
                transcriptData = data.transcriptData
                processScheduleLocally()
            } catch (e: Exception) {
                DebugLogger.log("DATA", "Error loading offline data: ${e.message}")
            }
        }
    }

    fun refresh() {
        if (isLoading) return
        refreshAllData(force = true)
    }

    fun onAppResume() {
        attemptAutoRefresh()
        checkForUpdates()
    }

    fun onNetworkAvailable() {
        attemptAutoRefresh()
    }

    private fun attemptAutoRefresh() {
        if (appState != AppConstants.STATE_APP || isLoading) return
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTime > refreshCooldownMs) {
            refreshAllData(force = false)
        }
    }

    private fun refreshAllData(force: Boolean, retryCount: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isRefreshing = true }
            try {
                val result = dataSyncManager!!.refreshCoreData()
                withContext(Dispatchers.Main) {
                    userData = result.userData; profileData = result.profileData
                    verify2FAStatus = result.verify2FA
                    if (result.newsList != null) newsList = result.newsList
                    if (result.payStatus != null) payStatus = result.payStatus
                }
                
                if (result.profileData != null) {
                    val schedResult = dataSyncManager!!.loadScheduleNetwork(result.profileData)
                    if (schedResult != null) {
                        withContext(Dispatchers.Main) {
                            timeMap = schedResult.timeMap
                            fullSchedule = schedResult.schedule
                            processScheduleLocally()
                        }
                    }
                    
                    try {
                        val session = dataSyncManager!!.fetchSession(result.profileData, appContext)
                        withContext(Dispatchers.Main) { sessionData = session }
                    } catch (_: Exception) {}
                }
                lastRefreshTime = System.currentTimeMillis()
                checkForUpdates()
            } catch (e: Exception) {
                val isAuthError = e.message?.contains("401") == true || e.message?.contains("HTTP 401") == true || e.message?.contains("Unauthenticated") == true
                if (isAuthError && retryCount == 0) {
                    val reloginSuccess = dataSyncManager!!.performSilentLogin(loginEmail, loginPass)
                    if (reloginSuccess && rememberMe) {
                        refreshAllData(force, retryCount = 1)
                        return@launch
                    } else {
                        withContext(Dispatchers.Main) { logout() }
                    }
                } else if (isAuthError) {
                    withContext(Dispatchers.Main) { logout() }
                }
            } finally {
                withContext(Dispatchers.Main) { isRefreshing = false; isLoading = false }
            }
        }
    }

    private fun processScheduleLocally() {
        val result = dataSyncManager?.processScheduleLocally(fullSchedule, language, appContext) ?: return
        determinedStream = result.determinedStream
        determinedGroup = result.determinedGroup
        todayDayName = result.todayDayName
        todayClasses = result.todayClasses
    }

    fun getSubjectTypeResId(item: ScheduleItem): Int? {
        val rawEn = item.subject_type?.name_en ?: ""
        val rawRu = item.subject_type?.name_ru ?: ""
        return when {
            rawEn.contains(AppConstants.SUBJECT_LECTION_EN, ignoreCase = true) || rawRu.equals(AppConstants.SUBJECT_LECTURE_RU, ignoreCase = true) -> R.string.type_lecture
            rawEn.contains(AppConstants.SUBJECT_PRACTICAL_EN, ignoreCase = true) || rawRu.contains(AppConstants.SUBJECT_PRACTICAL_RU, ignoreCase = true) -> R.string.type_practice
            rawEn.contains(AppConstants.SUBJECT_LAB_EN, ignoreCase = true) || rawRu.contains(AppConstants.SUBJECT_LAB_RU, ignoreCase = true) -> R.string.type_lab
            else -> null
        }
    }

    fun getTimeString(lessonId: Int): String = timeMap[lessonId] ?: "$lessonId"

    // ==================== TUITION ====================
    fun fetchTuitionDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isTuitionLoading = true; showTuitionSheet = true }
                val allPayments = dataSyncManager!!.fetchTuitionDetails()
                withContext(Dispatchers.Main) { tuitionDetails = allPayments }
            } catch (e: Exception) {
                DebugLogger.log("TUITION", "Failed to fetch tuition: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) { isTuitionLoading = false }
            }
        }
    }

    // ==================== JOURNAL ====================
    fun openJournal(subject: SessionSubjectWrapper, semesterId: Int? = null) {
        selectedJournalSubject = subject
        if (semesterId != null) selectedSemesterId = semesterId
        selectedJournalType = 1
        showJournalSheet = true
        fetchJournal()
    }
    
    fun changeJournalType(typeId: Int) {
        selectedJournalType = typeId
        fetchJournal()
    }
    
    fun fetchJournal() {
        val subject = selectedJournalSubject ?: return
        val curriculaId = subject.idCurricula ?: subject.marklist?.id?.toInt() ?: subject.subject?.id
        if (curriculaId == null) {
            DebugLogger.log("JOURNAL", "All ID fields are null, cannot fetch journal")
            viewModelScope.launch(Dispatchers.Main) { isJournalLoading = false; journalList = emptyList() }
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { isJournalLoading = true }
                val semesterId = selectedSemesterId ?: profileData?.active_semester ?: 1
                val activeSemester = profileData?.active_semester ?: semesterId

                // Load cached data first (offline support)
                val cached = dataSyncManager!!.loadCachedJournal(curriculaId, semesterId, selectedJournalType, activeSemester)
                if (cached.isNotEmpty()) {
                    withContext(Dispatchers.Main) { journalList = cached }
                    DebugLogger.log("JOURNAL", "Loaded ${cached.size} cached journal entries")
                }

                // Try network fetch
                val journal = dataSyncManager!!.fetchJournal(curriculaId, semesterId, selectedJournalType, activeSemester)
                withContext(Dispatchers.Main) { journalList = journal }
            } catch (e: Exception) {
                DebugLogger.log("JOURNAL", "Failed to fetch journal: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) { isJournalLoading = false }
            }
        }
    }

    // ==================== TRANSCRIPT ====================
    fun fetchTranscript() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load cached first
                val cached = dataSyncManager!!.let {
                    withContext(Dispatchers.IO) { prefs?.getRepository()?.getTranscriptSync() ?: emptyList() }
                }
                withContext(Dispatchers.Main) { isTranscriptLoading = true; showTranscriptScreen = true; transcriptData = cached }
                
                val uid = userData?.id ?: return@launch
                val movId = profileData?.studentMovement?.id ?: return@launch
                val transcript = dataSyncManager!!.fetchTranscript(uid, movId)
                withContext(Dispatchers.Main) { transcriptData = transcript }
            } catch (e: Exception) { } finally { withContext(Dispatchers.Main) { isTranscriptLoading = false } }
        }
    }

    // ==================== UPDATE ====================
    fun checkForUpdates() {
        if (appContext == null) return
        viewModelScope.launch {
            updateAvailableRelease = updateManager.checkForUpdate(appContext!!)
        }
    }

    fun downloadUpdate(context: Context) {
        val release = updateAvailableRelease ?: return
        val id = updateManager.startDownload(context, release) ?: return
        downloadId = id
        isUpdateDownloading = true
        updateProgress = 0f
        isUpdateReady = false
        monitorUpdateDownload(context, id)
    }

    private fun monitorUpdateDownload(context: Context, id: Long) {
        updateDownloadJob?.cancel()
        updateDownloadJob = viewModelScope.launch(Dispatchers.IO) {
            updateManager.monitorDownload(context, id) { status ->
                withContext(Dispatchers.Main) {
                    updateProgress = status.progress
                    if (status.isComplete) {
                        isUpdateDownloading = false
                        isUpdateReady = true
                        updateLocalUri = status.contentUri
                    } else if (status.isFailed) {
                        isUpdateDownloading = false
                        android.widget.Toast.makeText(context, context.getString(R.string.update_error_generic), android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun cancelUpdate(context: Context) {
        updateDownloadJob?.cancel()
        downloadId?.let { updateManager.cancelDownload(context, it) }
        isUpdateDownloading = false
        updateProgress = 0f
        isUpdateReady = false
        downloadId = null
    }

    fun triggerInstallUpdate(context: Context) {
        updateLocalUri?.let { updateManager.installUpdate(context, it) }
    }

    // ==================== PDF ====================
    fun cancelPdfGeneration() {
        pdfGenerationJob?.cancel()
        isPdfGenerating = false
        pdfStatusMessage = null
    }

    fun clearPdfState() {
        generatedPdfUri = null
        pdfStatusMessage = null
        isPdfGenerating = false
    }

    fun generateTranscriptPdf(context: Context, lang: String) {
        if (isPdfGenerating) return
        val studentId = userData?.id ?: return
        val movId = profileData?.studentMovement?.id ?: 0L
        
        pdfGenerationJob = viewModelScope.launch {
            isPdfGenerating = true
            generatedPdfUri = null
            if (dictionaryMap.isEmpty()) loadLocalDictionary()
            val uri = pdfManager!!.generateTranscriptPdf(context, studentId, movId, lang, dictionaryMap) { msg ->
                withContext(Dispatchers.Main) { pdfStatusMessage = msg }
            }
            generatedPdfUri = uri
            isPdfGenerating = false
        }
    }

    fun generateReferencePdf(context: Context, lang: String) {
        if (isPdfGenerating) return
        val studentId = userData?.id ?: return
        
        pdfGenerationJob = viewModelScope.launch {
            isPdfGenerating = true
            generatedPdfUri = null
            if (dictionaryMap.isEmpty()) loadLocalDictionary()
            val uri = pdfManager!!.generateReferencePdf(context, studentId, profileData, prefs?.getToken(), lang, dictionaryMap) { msg ->
                withContext(Dispatchers.Main) { pdfStatusMessage = msg }
            }
            generatedPdfUri = uri
            isPdfGenerating = false
        }
    }
}

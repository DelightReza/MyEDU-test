package myedu.oshsu.kg

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages the list of saved (logged-in) accounts.
 *
 * Account list is persisted in the "myedu_accounts" SharedPreferences file.
 * Per-account session data (used for background-sync comparison) lives in
 * a separate "myedu_cache_{id}" SharedPreferences file per account so it
 * never collides with the active account's primary cache.
 */
class AccountManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("myedu_accounts", Context.MODE_PRIVATE)
    private val gson = Gson()

    // ── Account list ─────────────────────────────────────────────────────────

    fun getAllAccounts(): List<SavedAccount> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SavedAccount>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOrUpdateAccount(account: SavedAccount) {
        val accounts = getAllAccounts().toMutableList()
        val idx = accounts.indexOfFirst { it.id == account.id }
        if (idx >= 0) accounts[idx] = account else accounts.add(account)
        prefs.edit().putString(KEY_ACCOUNTS, gson.toJson(accounts)).apply()
    }

    fun removeAccount(id: String) {
        val accounts = getAllAccounts().toMutableList()
        accounts.removeAll { it.id == id }
        val editor = prefs.edit().putString(KEY_ACCOUNTS, gson.toJson(accounts))
        if (getActiveAccountId() == id) editor.remove(KEY_ACTIVE_ID)
        editor.apply()
    }

    // ── Active account ────────────────────────────────────────────────────────

    fun getActiveAccountId(): String? = prefs.getString(KEY_ACTIVE_ID, null)

    fun getActiveAccount(): SavedAccount? {
        val id = getActiveAccountId() ?: return null
        return getAllAccounts().find { it.id == id }
    }

    fun setActiveAccount(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    // ── Per-account session cache (for background sync comparison) ───────────

    /**
     * Returns a SharedPreferences file dedicated to [accountId].
     * Background sync stores the last-seen session_list here so it can
     * detect grade/portal changes without touching the active account's cache.
     */
    fun getAccountPrefs(accountId: String): SharedPreferences =
        context.getSharedPreferences("myedu_cache_$accountId", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCOUNTS = "accounts_list"
        private const val KEY_ACTIVE_ID = "active_account_id"
    }
}

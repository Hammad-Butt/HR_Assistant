package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.engine.PolicyEngine
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

class HRRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hr_assistant_prefs", Context.MODE_PRIVATE)

    var isUnlocked: Boolean = false

    fun getIsDarkTheme(): Boolean {
        return prefs.getBoolean("is_dark_theme", true)
    }

    fun setIsDarkTheme(isDark: Boolean) {
        prefs.edit().putBoolean("is_dark_theme", isDark).apply()
    }

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(("hr-assistant:" + pin.trim()).toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun getHrPin(): String? {
        return prefs.getString("hr_pin", null)
    }

    fun setHrPin(hashedPin: String) {
        prefs.edit().putString("hr_pin", hashedPin).apply()
        isUnlocked = true
    }

    fun checkPin(enteredPin: String): Boolean {
        val stored = getHrPin() ?: return false
        val computed = hashPin(enteredPin)
        val valid = computed == stored || enteredPin == stored // migration support
        if (valid) {
            isUnlocked = true
        }
        return valid
    }

    fun loadCompanySettings(): CompanySettings {
        return CompanySettings(
            orgName = prefs.getString("org_name", "") ?: "",
            orgLogoUri = prefs.getString("org_logo_uri", null),
            hrMail = prefs.getString("hr_mail", "") ?: ""
        )
    }

    fun saveCompanySettings(settings: CompanySettings) {
        prefs.edit()
            .putString("org_name", settings.orgName)
            .putString("org_logo_uri", settings.orgLogoUri)
            .putString("hr_mail", settings.hrMail)
            .apply()
    }

    fun loadApiSettings(): ApiSettings {
        val keysJson = prefs.getString("api_keys_json", null)
        val keysList = mutableListOf<ApiKeyItem>()
        if (!keysJson.isNullOrEmpty()) {
            try {
                val arr = JSONArray(keysJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val providerStr = obj.optString("provider", ApiProvider.GROQ.name)
                    val provider = try {
                        ApiProvider.valueOf(providerStr)
                    } catch (_: Exception) {
                        ApiProvider.GROQ
                    }
                    keysList.add(
                        ApiKeyItem(
                            id = obj.getString("id"),
                            provider = provider,
                            apiKey = obj.getString("apiKey"),
                            model = obj.optString("model", ""),
                            customUrl = obj.optString("customUrl", ""),
                            isExhausted = obj.optBoolean("isExhausted", false),
                            addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        return ApiSettings(
            keys = keysList,
            legacyApiKey = prefs.getString("api_key", "") ?: "",
            legacyApiUrl = prefs.getString("api_url", "https://api.openai.com/v1/chat/completions") ?: "https://api.openai.com/v1/chat/completions",
            legacyModel = prefs.getString("api_model", "gpt-4o-mini") ?: "gpt-4o-mini"
        )
    }

    fun saveApiSettings(settings: ApiSettings) {
        val arr = JSONArray()
        for (item in settings.keys) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("provider", item.provider.name)
            obj.put("apiKey", item.apiKey)
            obj.put("model", item.model)
            obj.put("customUrl", item.customUrl)
            obj.put("isExhausted", item.isExhausted)
            obj.put("addedAt", item.addedAt)
            arr.put(obj)
        }

        prefs.edit()
            .putString("api_keys_json", arr.toString())
            .putString("api_key", settings.legacyApiKey)
            .putString("api_url", settings.legacyApiUrl)
            .putString("api_model", settings.legacyModel)
            .apply()
    }

    fun loadDocuments(): List<DocumentItem> {
        val jsonStr = prefs.getString("documents_json", null)
        if (jsonStr == null) {
            // First run: load default handbook
            val defaultDoc = PolicyEngine.getDefaultHandbookDocument()
            val list = listOf(defaultDoc)
            saveDocuments(list)
            return list
        }
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<DocumentItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val chunksArr = obj.getJSONArray("chunks")
                val chunks = mutableListOf<String>()
                for (j in 0 until chunksArr.length()) {
                    chunks.add(chunksArr.getString(j))
                }
                list.add(
                    DocumentItem(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        chunks = chunks,
                        uploadedAt = obj.getLong("uploadedAt"),
                        size = obj.getLong("size")
                    )
                )
            }
            list
        } catch (e: Exception) {
            listOf(PolicyEngine.getDefaultHandbookDocument())
        }
    }

    fun saveDocuments(docs: List<DocumentItem>) {
        val array = JSONArray()
        for (doc in docs) {
            val obj = JSONObject()
            obj.put("id", doc.id)
            obj.put("name", doc.name)
            val chunksArr = JSONArray()
            doc.chunks.forEach { chunksArr.put(it) }
            obj.put("chunks", chunksArr)
            obj.put("uploadedAt", doc.uploadedAt)
            obj.put("size", doc.size)
            array.put(obj)
        }
        prefs.edit().putString("documents_json", array.toString()).apply()
    }

    fun loadChatHistory(): List<ChatMessage> {
        val jsonStr = prefs.getString("chat_history_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ChatMessage>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val bulletsList = if (obj.has("bullets")) {
                    val bArr = obj.getJSONArray("bullets")
                    (0 until bArr.length()).map { bArr.getString(it) }
                } else null

                val qTermsList = if (obj.has("qTerms")) {
                    val qArr = obj.getJSONArray("qTerms")
                    (0 until qArr.length()).map { qArr.getString(it) }
                } else emptyList()

                list.add(
                    ChatMessage(
                        id = obj.getString("id"),
                        role = obj.getString("role"),
                        text = obj.optString("text", ""),
                        isRefusal = obj.optBoolean("isRefusal", false),
                        title = if (obj.has("title")) obj.getString("title") else null,
                        bullets = bulletsList,
                        sourceName = if (obj.has("sourceName")) obj.getString("sourceName") else null,
                        sourceText = if (obj.has("sourceText")) obj.getString("sourceText") else null,
                        qTerms = qTermsList,
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveChatHistory(history: List<ChatMessage>) {
        val limited = history.takeLast(80)
        val array = JSONArray()
        for (msg in limited) {
            val obj = JSONObject()
            obj.put("id", msg.id)
            obj.put("role", msg.role)
            obj.put("text", msg.text)
            obj.put("isRefusal", msg.isRefusal)
            msg.title?.let { obj.put("title", it) }
            msg.bullets?.let { b ->
                val bArr = JSONArray()
                b.forEach { bArr.put(it) }
                obj.put("bullets", bArr)
            }
            msg.sourceName?.let { obj.put("sourceName", it) }
            msg.sourceText?.let { obj.put("sourceText", it.take(900)) }
            if (msg.qTerms.isNotEmpty()) {
                val qArr = JSONArray()
                msg.qTerms.forEach { qArr.put(it) }
                obj.put("qTerms", qArr)
            }
            obj.put("timestamp", msg.timestamp)
            array.put(obj)
        }
        prefs.edit().putString("chat_history_json", array.toString()).apply()
    }

    fun clearChatHistory() {
        prefs.edit().remove("chat_history_json").apply()
    }

    fun loadSupabaseSettings(): SupabaseSettings {
        val fallbackUrl = "https://uaotrgxbyntiybumoeks.supabase.co"
        val fallbackKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVhb3RyZ3hieW50aXlidW1vZWtzIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODYxMTQwNTEsImV4cCI6MjEwMTY5MDA1MX0.L4SsIeMSOa1ZiKjX5KQTzjZtDomIwcnyUB6gW1iAEEA"

        val defaultUrl = try {
            com.example.BuildConfig.SUPABASE_URL.ifBlank { fallbackUrl }
        } catch (_: Throwable) {
            fallbackUrl
        }

        val defaultKey = try {
            com.example.BuildConfig.SUPABASE_ANON_KEY.ifBlank { fallbackKey }
        } catch (_: Throwable) {
            fallbackKey
        }

        return SupabaseSettings(
            projectUrl = prefs.getString("supabase_url", defaultUrl) ?: defaultUrl,
            anonKey = prefs.getString("supabase_anon_key", defaultKey) ?: defaultKey,
            isAutoSyncEnabled = prefs.getBoolean("supabase_auto_sync", true),
            lastSyncTime = prefs.getLong("supabase_last_sync", 0L)
        )
    }

    fun saveSupabaseSettings(settings: SupabaseSettings) {
        prefs.edit()
            .putString("supabase_url", settings.projectUrl.trim())
            .putString("supabase_anon_key", settings.anonKey.trim())
            .putBoolean("supabase_auto_sync", settings.isAutoSyncEnabled)
            .putLong("supabase_last_sync", settings.lastSyncTime)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("is_logged_in", false)
    }

    fun getCurrentUserEmail(): String {
        return prefs.getString("current_user_email", "") ?: ""
    }

    fun getCurrentUserRole(): String {
        return prefs.getString("current_user_role", "EMPLOYEE") ?: "EMPLOYEE"
    }

    fun saveSession(email: String, role: String) {
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("current_user_email", email.trim().lowercase())
            .putString("current_user_role", role)
            .apply()
    }

    fun clearSession() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("current_user_email")
            .remove("current_user_role")
            .apply()
    }

    fun getHrLoginEmail(): String {
        return prefs.getString("hr_login_email", "hr@apex.com") ?: "hr@apex.com"
    }

    fun getHrLoginPassword(): String {
        return prefs.getString("hr_login_password", "Admin@123") ?: "Admin@123"
    }

    fun saveHrCredentials(email: String, pass: String) {
        prefs.edit()
            .putString("hr_login_email", email.trim().lowercase())
            .putString("hr_login_password", pass.trim())
            .apply()
    }

    fun saveLocalUser(account: UserAccount) {
        val users = getAllLocalUsers().toMutableList()
        users.removeAll { it.email.equals(account.email, ignoreCase = true) }
        users.add(account)
        val arr = JSONArray()
        for (u in users) {
            val obj = JSONObject().apply {
                put("email", u.email.trim().lowercase())
                put("password", u.password)
                put("role", u.role)
                put("createdAt", u.createdAt)
                put("username", u.username)
            }
            arr.put(obj)
        }
        prefs.edit().putString("local_users_json", arr.toString()).apply()
    }

    fun getLocalUser(email: String): UserAccount? {
        val normalized = email.trim().lowercase()
        return getAllLocalUsers().find { it.email.equals(normalized, ignoreCase = true) }
    }

    fun getAllLocalUsers(): List<UserAccount> {
        val jsonStr = prefs.getString("local_users_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<UserAccount>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    UserAccount(
                        email = obj.getString("email"),
                        password = obj.getString("password"),
                        role = obj.optString("role", "EMPLOYEE"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        username = obj.optString("username", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun factoryReset() {
        prefs.edit().clear().apply()
    }
}


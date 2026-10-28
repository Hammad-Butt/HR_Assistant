package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiSettings
import com.example.data.ChatMessage
import com.example.data.CompanySettings
import com.example.data.DocumentItem
import com.example.data.HRRepository
import com.example.engine.DocParser
import com.example.engine.PolicyEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class ModalState {
    data object None : ModalState()
    data object ClearChat : ModalState()
    data class EmailHr(val hrEmail: String, val lastQuestionRefusal: String?) : ModalState()
    data object HrEmailNotSet : ModalState()
    data class DocPreview(val doc: DocumentItem) : ModalState()
    data class ConfirmDeleteDoc(val doc: DocumentItem) : ModalState()
    data object ConfirmWipeDocs : ModalState()
    data class ShowSqlDialog(val sql: String) : ModalState()
}

data class HRUiState(
    val docs: List<DocumentItem> = emptyList(),
    val chatHistory: List<ChatMessage> = emptyList(),
    val companySettings: CompanySettings = CompanySettings(),
    val apiSettings: ApiSettings = ApiSettings(),
    val supabaseSettings: com.example.data.SupabaseSettings = com.example.data.SupabaseSettings(),
    val hrPinHash: String? = null,
    val isUnlocked: Boolean = false,
    val isLoggedIn: Boolean = false,
    val currentUserEmail: String = "",
    val currentUserRole: String = "EMPLOYEE",
    val authError: String = "",
    val isSettingsOpen: Boolean = false,
    val currentTab: Int = 0,
    val isBooting: Boolean = true,
    val isDarkTheme: Boolean = true,
    val isTyping: Boolean = false,
    val isSyncing: Boolean = false,
    val statusText: String = "",
    val isStatusError: Boolean = false,
    val modalState: ModalState = ModalState.None,
    val suggestions: List<String> = emptyList(),
    val pinError: String = "",
    val orgError: String = "",
    val hrLoginEmail: String = "hr@apex.com",
    val hrLoginPassword: String = "Admin@123",
    val toastMessage: String? = null
)


class HRViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HRRepository(application)
    private val _uiState = MutableStateFlow(HRUiState())
    val uiState: StateFlow<HRUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        // Show loading screen for exactly 3 seconds first then the actual interface
        viewModelScope.launch {
            delay(3000)
            _uiState.update { it.copy(isBooting = false) }
            // Perform initial document sync with Supabase
            if (_uiState.value.supabaseSettings.isConfigured && _uiState.value.supabaseSettings.isAutoSyncEnabled) {
                syncWithSupabase(isManual = false)
            }
        }

        // Periodic background sync loop every 45 seconds for real-time document synchronization
        viewModelScope.launch {
            while (true) {
                delay(45_000)
                if (_uiState.value.supabaseSettings.isConfigured &&
                    _uiState.value.supabaseSettings.isAutoSyncEnabled &&
                    !_uiState.value.isSyncing) {
                    syncWithSupabase(isManual = false)
                }
            }
        }
    }

    private fun loadInitialData() {
        val docs = repository.loadDocuments()
        val history = repository.loadChatHistory()
        val company = repository.loadCompanySettings()
        val api = repository.loadApiSettings()
        val supabase = repository.loadSupabaseSettings()
        val pin = repository.getHrPin()
        val hrEmail = repository.getHrLoginEmail()
        val hrPass = repository.getHrLoginPassword()
        val suggestions = PolicyEngine.getSuggestions(docs)
        val isDark = repository.getIsDarkTheme()
        val isLoggedIn = repository.isLoggedIn()
        val email = repository.getCurrentUserEmail()
        val role = repository.getCurrentUserRole()

        val status = if (docs.isNotEmpty()) {
            "${docs.size} document${if (docs.size == 1) "" else "s"} loaded"
        } else {
            "No documents loaded"
        }

        _uiState.update {
            it.copy(
                docs = docs,
                chatHistory = history,
                companySettings = company,
                apiSettings = api,
                supabaseSettings = supabase,
                hrPinHash = pin,
                hrLoginEmail = hrEmail,
                hrLoginPassword = hrPass,
                isUnlocked = repository.isUnlocked,
                isLoggedIn = isLoggedIn,
                currentUserEmail = email,
                currentUserRole = role,
                isDarkTheme = isDark,
                statusText = status,
                suggestions = suggestions
            )
        }
    }

    fun login(emailInput: String, passInput: String) {
        val email = emailInput.trim().lowercase()
        val pass = passInput.trim()
        if (email.isEmpty() || pass.isEmpty()) {
            _uiState.update { it.copy(authError = "Please enter email and password") }
            return
        }

        val storedHrEmail = repository.getHrLoginEmail()
        val storedHrPass = repository.getHrLoginPassword()
        if (email == storedHrEmail && pass == storedHrPass) {
            repository.saveSession(email, "HR")
            val updatedCompany = _uiState.value.companySettings.copy(hrMail = email)
            repository.saveCompanySettings(updatedCompany)
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    currentUserEmail = email,
                    currentUserRole = "HR",
                    companySettings = updatedCompany,
                    authError = ""
                )
            }
            viewModelScope.launch {
                val hrAccount = com.example.data.UserAccount(
                    email = email,
                    password = pass,
                    role = "HR",
                    createdAt = System.currentTimeMillis()
                )
                com.example.data.SupabaseService.registerUser(_uiState.value.supabaseSettings, hrAccount)
            }
            showToast("Logged in as HR")
            return
        }

        // Check locally saved user accounts first
        val localUser = repository.getLocalUser(email)
        if (localUser != null && localUser.password == pass) {
            repository.saveSession(localUser.email, localUser.role)
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    currentUserEmail = localUser.email,
                    currentUserRole = localUser.role,
                    authError = ""
                )
            }
            showToast("Welcome back, ${localUser.email}!")
            return
        }

        // Check standard demo employee
        if (email == "employee@apex.com" && pass == "Employee123") {
            repository.saveSession(email, "EMPLOYEE")
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    currentUserEmail = email,
                    currentUserRole = "EMPLOYEE",
                    authError = ""
                )
            }
            showToast("Logged in as Employee")
            return
        }

        // Query Supabase Cloud
        viewModelScope.launch {
            _uiState.update { it.copy(authError = "Authenticating...") }
            val result = com.example.data.SupabaseService.fetchUser(_uiState.value.supabaseSettings, email)
            result.onSuccess { user ->
                if (user != null && user.password == pass) {
                    repository.saveLocalUser(user)
                    repository.saveSession(user.email, user.role)
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            currentUserEmail = user.email,
                            currentUserRole = user.role,
                            authError = ""
                        )
                    }
                    showToast("Welcome back, ${user.email}!")
                } else {
                    _uiState.update { it.copy(authError = "Invalid email or password. HR default: ${repository.getHrLoginEmail()} / ${repository.getHrLoginPassword()}") }
                }
            }.onFailure { err ->
                _uiState.update { it.copy(authError = "Login failed: ${err.message}") }
            }
        }
    }

    fun signUp(usernameInput: String = "", emailInput: String, passInput: String) {
        val email = emailInput.trim().lowercase()
        val pass = passInput.trim()
        val username = usernameInput.trim()
        if (email.isEmpty() || pass.isEmpty()) {
            _uiState.update { it.copy(authError = "Please enter email and password") }
            return
        }

        if (pass.length < 4) {
            _uiState.update { it.copy(authError = "Password must be at least 4 characters") }
            return
        }

        val account = com.example.data.UserAccount(
            email = email,
            password = pass,
            role = "EMPLOYEE",
            createdAt = System.currentTimeMillis(),
            username = username
        )

        // Save locally first so the employee can immediately log in on mobile without being blocked
        repository.saveLocalUser(account)
        repository.saveSession(email, "EMPLOYEE")
        _uiState.update {
            it.copy(
                isLoggedIn = true,
                currentUserEmail = email,
                currentUserRole = "EMPLOYEE",
                authError = ""
            )
        }
        showToast("Employee account created!")

        // Sync with Supabase in background silently
        val supabase = _uiState.value.supabaseSettings
        if (supabase.isConfigured) {
            viewModelScope.launch {
                val result = com.example.data.SupabaseService.registerUser(supabase, account)
                result.onSuccess {
                    showToast("Synced with Supabase Cloud ✓")
                }.onFailure { err ->
                    android.util.Log.w("HRViewModel", "Supabase sync error on signup: ${err.message}")
                }
            }
        }
    }

    fun logout() {
        repository.clearSession()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                currentUserEmail = "",
                currentUserRole = "EMPLOYEE",
                isSettingsOpen = false
            )
        }
        showToast("Logged out successfully")
    }


    fun toggleTheme() {
        val next = !_uiState.value.isDarkTheme
        repository.setIsDarkTheme(next)
        _uiState.update { it.copy(isDarkTheme = next) }
    }

    fun sendMessage(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        val userMessage = ChatMessage(
            role = "user",
            text = trimmed,
            timestamp = System.currentTimeMillis()
        )

        val updatedHistory = _uiState.value.chatHistory + userMessage
        _uiState.update { it.copy(chatHistory = updatedHistory) }
        repository.saveChatHistory(updatedHistory)

        // Check greetings first
        val greeting = PolicyEngine.detectGreeting(trimmed)
        if (greeting != null) {
            val botMessage = ChatMessage(
                role = "bot",
                text = greeting,
                timestamp = System.currentTimeMillis()
            )
            appendBotMessage(botMessage)
            return
        }

        // Check if any documents loaded
        if (_uiState.value.docs.isEmpty()) {
            val refusalMsg = ChatMessage(
                role = "bot",
                text = "No HR documents have been uploaded on this device yet. Ask HR to upload the company policy files first.",
                isRefusal = true,
                timestamp = System.currentTimeMillis()
            )
            appendBotMessage(refusalMsg)
            return
        }

        // Processing response
        _uiState.update { it.copy(isTyping = true) }

        viewModelScope.launch {
            delay(350) // slight realistic response latency

            val answer = PolicyEngine.answerQuery(trimmed, _uiState.value.docs)

            val botMessage = if (answer.isRefusal) {
                ChatMessage(
                    role = "bot",
                    text = answer.refusalText.ifEmpty { PolicyEngine.REFUSAL_MESSAGE },
                    isRefusal = true,
                    timestamp = System.currentTimeMillis()
                )
            } else {
                // Attempt AI rewrite using Groq, Gemini, or custom endpoint with automatic silent key failover
                val rewritten = try {
                    com.example.engine.AiRouter.rewritePolicyAnswer(
                        query = trimmed,
                        contextPassages = answer.sourceText,
                        apiSettings = _uiState.value.apiSettings,
                        onKeyExhausted = { exhaustedKey ->
                            markKeyExhaustedSilently(exhaustedKey)
                        }
                    )
                } catch (_: Exception) {
                    null
                }

                ChatMessage(
                    role = "bot",
                    title = rewritten?.title ?: answer.title,
                    bullets = rewritten?.bullets ?: answer.bullets,
                    sourceName = answer.sourceName,
                    sourceText = answer.sourceText,
                    qTerms = answer.qTerms,
                    isRefusal = false,
                    timestamp = System.currentTimeMillis()
                )
            }

            _uiState.update { it.copy(isTyping = false) }
            appendBotMessage(botMessage)
        }
    }

    private fun appendBotMessage(message: ChatMessage) {
        val updated = _uiState.value.chatHistory + message
        _uiState.update { it.copy(chatHistory = updated) }
        repository.saveChatHistory(updated)
    }

    fun requestNewChat() {
        if (_uiState.value.chatHistory.isEmpty()) {
            showToast("Chat is already empty")
            return
        }
        _uiState.update { it.copy(modalState = ModalState.ClearChat) }
    }

    fun confirmClearChat() {
        _uiState.update { it.copy(chatHistory = emptyList(), modalState = ModalState.None) }
        repository.clearChatHistory()
        showToast("Chat cleared")
    }

    fun openSettings() {
        val isHr = _uiState.value.currentUserRole.equals("HR", ignoreCase = true)
        val initialTab = if (!isHr && _uiState.value.currentTab > 1) 0 else _uiState.value.currentTab
        _uiState.update { it.copy(isSettingsOpen = true, currentTab = initialTab, pinError = "", orgError = "") }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun selectTab(index: Int) {
        val isHr = _uiState.value.currentUserRole.equals("HR", ignoreCase = true)
        val safeIndex = if (!isHr && index > 1) 0 else index
        _uiState.update { it.copy(currentTab = safeIndex, pinError = "", orgError = "") }
    }

    fun unlockOrSetPin(enteredPin: String) {
        val trimmed = enteredPin.trim()
        if (trimmed.isEmpty()) {
            _uiState.update { it.copy(pinError = "Enter a PIN.") }
            return
        }

        val stored = _uiState.value.hrPinHash
        if (stored == null) {
            if (trimmed.length < 4) {
                _uiState.update { it.copy(pinError = "Use at least 4 characters.") }
                return
            }
            val hashed = repository.hashPin(trimmed)
            repository.setHrPin(hashed)
            _uiState.update { it.copy(hrPinHash = hashed, isUnlocked = true, pinError = "") }
            showToast("HR PIN set successfully")
            return
        }

        if (repository.checkPin(trimmed)) {
            _uiState.update { it.copy(isUnlocked = true, pinError = "") }
        } else {
            _uiState.update { it.copy(pinError = "Incorrect PIN.") }
        }
    }

    fun handleFileUpload(uris: List<Uri>, context: Context) {
        viewModelScope.launch {
            var addedCount = 0
            val addedDocs = mutableListOf<DocumentItem>()
            val currentDocs = _uiState.value.docs.toMutableList()

            for (uri in uris) {
                var fileName = "Document_${System.currentTimeMillis()}"
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                    }
                } catch (_: Exception) {}

                try {
                    val rawText = DocParser.parseUri(context, uri, fileName)
                    val chunks = PolicyEngine.chunkText(rawText)
                    if (chunks.isEmpty()) {
                        showToast("File $fileName was too short to use")
                        continue
                    }

                    // Replace if same name exists
                    currentDocs.removeAll { it.name == fileName }
                    val newDoc = DocumentItem(
                        id = "doc_" + System.currentTimeMillis() + "_" + (1000..9999).random(),
                        name = fileName,
                        chunks = chunks,
                        uploadedAt = System.currentTimeMillis(),
                        size = rawText.toByteArray().size.toLong()
                    )
                    currentDocs.add(newDoc)
                    addedDocs.add(newDoc)
                    addedCount++
                } catch (e: Exception) {
                    showToast("Failed to parse $fileName: ${e.message}")
                }
            }

            if (addedCount > 0) {
                repository.saveDocuments(currentDocs)
                val newSuggestions = PolicyEngine.getSuggestions(currentDocs)
                val status = "${currentDocs.size} document${if (currentDocs.size == 1) "" else "s"} loaded"
                _uiState.update {
                    it.copy(
                        docs = currentDocs,
                        suggestions = newSuggestions,
                        statusText = status
                    )
                }
                showToast("$addedCount document${if (addedCount == 1) "" else "s"} added")

                // Auto-sync uploaded documents to Supabase Cloud for all employees
                val supabase = _uiState.value.supabaseSettings
                if (supabase.isConfigured) {
                    viewModelScope.launch {
                        var cloudUploaded = 0
                        for (doc in addedDocs) {
                            val res = com.example.data.SupabaseService.upsertDocument(supabase, doc)
                            if (res.isSuccess) {
                                cloudUploaded++
                            } else {
                                val err = res.exceptionOrNull()
                                android.util.Log.w("HRViewModel", "Document cloud sync error: ${err?.message}")
                            }
                        }
                        if (cloudUploaded > 0) {
                            showToast("Synced $cloudUploaded document${if (cloudUploaded == 1) "" else "s"} to Supabase")
                            syncWithSupabase(isManual = false)
                        }
                    }
                }
            }
        }
    }

    fun reloadSampleHandbook() {
        val sample = PolicyEngine.getDefaultHandbookDocument()
        val currentDocs = _uiState.value.docs.toMutableList()
        currentDocs.removeAll { it.id == sample.id || it.name == sample.name }
        currentDocs.add(0, sample)
        repository.saveDocuments(currentDocs)
        val newSuggestions = PolicyEngine.getSuggestions(currentDocs)
        val status = "${currentDocs.size} document${if (currentDocs.size == 1) "" else "s"} loaded"
        _uiState.update {
            it.copy(
                docs = currentDocs,
                suggestions = newSuggestions,
                statusText = status
            )
        }
        showToast("Sample Handbook loaded")

        // Also sync sample to Supabase
        val supabase = _uiState.value.supabaseSettings
        if (supabase.isConfigured) {
            viewModelScope.launch {
                com.example.data.SupabaseService.upsertDocument(supabase, sample)
                syncWithSupabase(isManual = false)
            }
        }
    }

    fun promptDeleteDoc(doc: DocumentItem) {
        _uiState.update { it.copy(modalState = ModalState.ConfirmDeleteDoc(doc)) }
    }

    fun confirmDeleteDoc(doc: DocumentItem) {
        val updated = _uiState.value.docs.filter { it.id != doc.id }
        repository.saveDocuments(updated)
        val newSuggestions = PolicyEngine.getSuggestions(updated)
        val status = if (updated.isNotEmpty()) {
            "${updated.size} document${if (updated.size == 1) "" else "s"} loaded"
        } else {
            "No documents loaded"
        }
        _uiState.update {
            it.copy(
                docs = updated,
                suggestions = newSuggestions,
                statusText = status,
                modalState = ModalState.None
            )
        }
        showToast("Document removed")

        val supabase = _uiState.value.supabaseSettings
        if (supabase.isConfigured) {
            viewModelScope.launch {
                com.example.data.SupabaseService.deleteDocument(supabase, doc.id)
            }
        }
    }

    fun promptWipeAllDocs() {
        _uiState.update { it.copy(modalState = ModalState.ConfirmWipeDocs) }
    }

    fun confirmWipeAllDocs() {
        val updated = emptyList<DocumentItem>()
        repository.saveDocuments(updated)
        _uiState.update {
            it.copy(
                docs = updated,
                suggestions = emptyList(),
                statusText = "No documents loaded",
                modalState = ModalState.None
            )
        }
        showToast("All documents removed")

        val supabase = _uiState.value.supabaseSettings
        if (supabase.isConfigured) {
            viewModelScope.launch {
                com.example.data.SupabaseService.wipeAllDocuments(supabase)
            }
        }
    }


    fun previewDoc(doc: DocumentItem) {
        _uiState.update { it.copy(modalState = ModalState.DocPreview(doc)) }
    }

    fun saveCompanyDetails(orgName: String, hrMail: String) {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (hrMail.isNotBlank() && !emailRegex.matches(hrMail.trim())) {
            _uiState.update { it.copy(orgError = "Enter a valid email address.") }
            return
        }

        val updated = _uiState.value.companySettings.copy(
            orgName = orgName.trim(),
            hrMail = hrMail.trim()
        )
        repository.saveCompanySettings(updated)
        _uiState.update { it.copy(companySettings = updated, orgError = "") }
        showToast("Saved ✓")
    }

    fun setCompanyLogo(uri: Uri?) {
        val updated = _uiState.value.companySettings.copy(orgLogoUri = uri?.toString())
        repository.saveCompanySettings(updated)
        _uiState.update { it.copy(companySettings = updated) }
    }

    fun removeCompanyLogo() {
        val updated = _uiState.value.companySettings.copy(orgLogoUri = null)
        repository.saveCompanySettings(updated)
        _uiState.update { it.copy(companySettings = updated) }
    }

    fun updateHrCredentials(currentPassInput: String, newEmailInput: String, newPassInput: String, confirmPassInput: String) {
        val currentPass = currentPassInput.trim()
        val newEmail = newEmailInput.trim().lowercase()
        val newPass = newPassInput.trim()
        val confirmPass = confirmPassInput.trim()

        val storedPass = repository.getHrLoginPassword()
        if (currentPass.isEmpty() || currentPass != storedPass) {
            _uiState.update { it.copy(orgError = "Incorrect current password.") }
            return
        }

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (newEmail.isNotBlank() && !emailRegex.matches(newEmail)) {
            _uiState.update { it.copy(orgError = "Enter a valid new email address.") }
            return
        }

        if (newPass.isNotEmpty()) {
            if (newPass.length < 4) {
                _uiState.update { it.copy(orgError = "New password must be at least 4 characters.") }
                return
            }
            if (newPass != confirmPass) {
                _uiState.update { it.copy(orgError = "New passwords do not match.") }
                return
            }
        }

        val oldEmail = repository.getHrLoginEmail()
        val finalEmail = if (newEmail.isNotBlank()) newEmail else oldEmail
        val finalPass = if (newPass.isNotBlank()) newPass else storedPass

        repository.saveHrCredentials(finalEmail, finalPass)
        val updatedCompany = _uiState.value.companySettings.copy(hrMail = finalEmail)
        repository.saveCompanySettings(updatedCompany)

        _uiState.update { 
            it.copy(
                companySettings = updatedCompany,
                hrLoginEmail = finalEmail,
                hrLoginPassword = finalPass,
                currentUserEmail = if (it.currentUserRole.equals("HR", ignoreCase = true)) finalEmail else it.currentUserEmail,
                orgError = ""
            ) 
        }
        viewModelScope.launch {
            val supabase = _uiState.value.supabaseSettings
            if (supabase.isConfigured) {
                if (!oldEmail.equals(finalEmail, ignoreCase = true)) {
                    com.example.data.SupabaseService.deleteUser(supabase, oldEmail)
                }
                val hrAccount = com.example.data.UserAccount(
                    email = finalEmail,
                    password = finalPass,
                    role = "HR",
                    createdAt = System.currentTimeMillis(),
                    username = "hr_admin"
                )
                com.example.data.SupabaseService.registerUser(supabase, hrAccount)
            }
        }
        showToast("HR credentials updated successfully ✓")
    }

    fun factoryResetEverything() {
        repository.factoryReset()
        loadInitialData()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                currentUserEmail = "",
                currentUserRole = "EMPLOYEE",
                isSettingsOpen = false
            )
        }
        showToast("Factory reset complete. HR credentials reset to default (hr@apex.com / Admin@123).")
    }

    fun addApiKeys(provider: com.example.data.ApiProvider, keysInput: String, model: String, customUrl: String = "") {
        val rawKeys = keysInput.split(Regex("[\\n,]+")).map { it.trim() }.filter { it.isNotEmpty() }
        if (rawKeys.isEmpty()) {
            showToast("Enter at least one API key")
            return
        }

        val currentKeys = _uiState.value.apiSettings.keys.toMutableList()
        var addedCount = 0

        for (k in rawKeys) {
            if (currentKeys.none { it.apiKey == k && it.provider == provider }) {
                currentKeys.add(
                    com.example.data.ApiKeyItem(
                        provider = provider,
                        apiKey = k,
                        model = model.trim(),
                        customUrl = customUrl.trim()
                    )
                )
                addedCount++
            }
        }

        val updated = _uiState.value.apiSettings.copy(keys = currentKeys)
        repository.saveApiSettings(updated)
        _uiState.update { it.copy(apiSettings = updated) }
        showToast("$addedCount key${if (addedCount == 1) "" else "s"} added to pool")
    }

    fun removeApiKey(id: String) {
        val updatedKeys = _uiState.value.apiSettings.keys.filter { it.id != id }
        val updated = _uiState.value.apiSettings.copy(keys = updatedKeys)
        repository.saveApiSettings(updated)
        _uiState.update { it.copy(apiSettings = updated) }
        showToast("Key removed")
    }

    fun resetExhaustedKeys() {
        val updatedKeys = _uiState.value.apiSettings.keys.map { it.copy(isExhausted = false) }
        val updated = _uiState.value.apiSettings.copy(keys = updatedKeys)
        repository.saveApiSettings(updated)
        _uiState.update { it.copy(apiSettings = updated) }
        showToast("Reset rate limits for all keys")
    }

    fun clearAllApiKeys() {
        val updated = _uiState.value.apiSettings.copy(keys = emptyList(), legacyApiKey = "")
        repository.saveApiSettings(updated)
        _uiState.update { it.copy(apiSettings = updated) }
        showToast("All API keys removed")
    }

    fun markKeyExhaustedSilently(key: com.example.data.ApiKeyItem) {
        // Silently mark as exhausted in background so subsequent requests prefer healthy keys
        val updatedKeys = _uiState.value.apiSettings.keys.map {
            if (it.id == key.id || (it.apiKey == key.apiKey && it.provider == key.provider)) {
                it.copy(isExhausted = true)
            } else it
        }
        val updated = _uiState.value.apiSettings.copy(keys = updatedKeys)
        repository.saveApiSettings(updated)
        _uiState.update { it.copy(apiSettings = updated) }
        // Crucial: No toast or user-facing notification is triggered here!
    }

    fun saveApi(apiKey: String, apiUrl: String, model: String) {
        val updated = _uiState.value.apiSettings.copy(
            legacyApiKey = apiKey.trim(),
            legacyApiUrl = apiUrl.trim().ifEmpty { "https://api.openai.com/v1/chat/completions" },
            legacyModel = model.trim().ifEmpty { "gpt-4o-mini" }
        )
        repository.saveApiSettings(updated)
        _uiState.update { it.copy(apiSettings = updated) }
        showToast("Saved ✓")
    }

    fun clearApi() {
        val defaultApi = ApiSettings()
        repository.saveApiSettings(defaultApi)
        _uiState.update { it.copy(apiSettings = defaultApi) }
        showToast("API keys cleared")
    }

    fun handleMailClick() {
        val mail = _uiState.value.companySettings.hrMail
        if (mail.isBlank()) {
            _uiState.update { it.copy(modalState = ModalState.HrEmailNotSet) }
        } else {
            val history = _uiState.value.chatHistory
            val lastRefusal = if (history.isNotEmpty() && history.last().isRefusal && history.size >= 2) {
                history[history.size - 2].text
            } else null

            _uiState.update { it.copy(modalState = ModalState.EmailHr(mail, lastRefusal)) }
        }
    }

    fun copyAnswerToClipboard(context: Context, title: String, bullets: List<String>) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val text = buildString {
            appendLine(title)
            appendLine()
            bullets.forEach { appendLine("• $it") }
        }.trimEnd()
        val clip = ClipData.newPlainText("HR Policy Answer", text)
        clipboard.setPrimaryClip(clip)
        showToast("Copied to clipboard")
    }

    fun syncWithSupabase(isManual: Boolean = false) {
        val config = _uiState.value.supabaseSettings
        if (!config.isConfigured) {
            if (isManual) showToast("Supabase is not configured yet")
            return
        }

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isSyncing = true,
                    supabaseSettings = it.supabaseSettings.copy(syncStatus = com.example.data.SyncStatus.SYNCING)
                ) 
            }

            val fetchResult = com.example.data.SupabaseService.fetchAllDocuments(config)
            fetchResult.onSuccess { remoteDocs ->
                val localDocs = _uiState.value.docs
                if (remoteDocs.isNotEmpty()) {
                    // Update app state with latest synced documents from HR
                    repository.saveDocuments(remoteDocs)
                    val newSuggestions = PolicyEngine.getSuggestions(remoteDocs)
                    val status = "${remoteDocs.size} document${if (remoteDocs.size == 1) "" else "s"} loaded (Cloud Synced)"
                    val updatedSettings = config.copy(
                        lastSyncTime = System.currentTimeMillis(),
                        syncStatus = com.example.data.SyncStatus.SUCCESS,
                        statusMessage = "Synced ${remoteDocs.size} policy documents"
                    )
                    repository.saveSupabaseSettings(updatedSettings)

                    _uiState.update {
                        it.copy(
                            docs = remoteDocs,
                            suggestions = newSuggestions,
                            statusText = status,
                            isSyncing = false,
                            supabaseSettings = updatedSettings
                        )
                    }
                    if (isManual) {
                        showToast("Synced ${remoteDocs.size} documents from Supabase")
                    }
                } else if (localDocs.isNotEmpty() && _uiState.value.isUnlocked) {
                    // Remote is currently empty, but HR is logged in and has local docs -> upload to Supabase!
                    for (doc in localDocs) {
                        com.example.data.SupabaseService.upsertDocument(config, doc)
                    }
                    val updatedSettings = config.copy(
                        lastSyncTime = System.currentTimeMillis(),
                        syncStatus = com.example.data.SyncStatus.SUCCESS,
                        statusMessage = "Uploaded ${localDocs.size} local documents to Cloud"
                    )
                    repository.saveSupabaseSettings(updatedSettings)
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            supabaseSettings = updatedSettings
                        )
                    }
                    if (isManual) {
                        showToast("Uploaded ${localDocs.size} documents to Supabase Cloud")
                    }
                } else {
                    val updatedSettings = config.copy(
                        lastSyncTime = System.currentTimeMillis(),
                        syncStatus = com.example.data.SyncStatus.SUCCESS,
                        statusMessage = "No documents found on Supabase"
                    )
                    repository.saveSupabaseSettings(updatedSettings)
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            supabaseSettings = updatedSettings
                        )
                    }
                    if (isManual) {
                        showToast("Connected to Supabase (0 documents uploaded yet)")
                    }
                }
            }.onFailure { err ->
                val isMissing = err is com.example.data.MissingTableException
                val updatedSettings = config.copy(
                    syncStatus = if (isMissing) com.example.data.SyncStatus.NEED_TABLE_SETUP else com.example.data.SyncStatus.ERROR,
                    statusMessage = if (isMissing) "Table `company_documents` not found" else (err.message ?: "Sync failed")
                )
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        supabaseSettings = updatedSettings
                    )
                }
                if (isManual) {
                    showToast("Sync completed (Local mode active)")
                }
            }
        }
    }

    fun saveSupabaseConfig(url: String, anonKey: String, isAutoSync: Boolean) {
        val updated = _uiState.value.supabaseSettings.copy(
            projectUrl = url.trim(),
            anonKey = anonKey.trim(),
            isAutoSyncEnabled = isAutoSync
        )
        repository.saveSupabaseSettings(updated)
        _uiState.update { it.copy(supabaseSettings = updated) }
        showToast("Supabase configuration saved")
        syncWithSupabase(isManual = true)
    }

    fun testSupabaseConnection() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val result = com.example.data.SupabaseService.testConnection(_uiState.value.supabaseSettings)
            _uiState.update { it.copy(isSyncing = false) }
            if (result.needsTableSetup) {
                _uiState.update { 
                    it.copy(
                        supabaseSettings = it.supabaseSettings.copy(
                            syncStatus = com.example.data.SyncStatus.NEED_TABLE_SETUP,
                            statusMessage = result.message
                        )
                    )
                }
                showToast("Connected to Supabase! Table setup needed (use Copy Setup SQL)")
            } else if (result.success) {
                _uiState.update {
                    it.copy(
                        supabaseSettings = it.supabaseSettings.copy(
                            syncStatus = com.example.data.SyncStatus.SUCCESS,
                            statusMessage = result.message
                        )
                    )
                }
                showToast("Connection successful ✓")
                syncWithSupabase(isManual = false)
            } else {
                _uiState.update {
                    it.copy(
                        supabaseSettings = it.supabaseSettings.copy(
                            syncStatus = com.example.data.SyncStatus.ERROR,
                            statusMessage = result.message
                        )
                    )
                }
                showToast(result.message)
            }
        }
    }

    fun copySqlToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Supabase SQL", com.example.data.SupabaseService.TABLE_SQL)
        clipboard.setPrimaryClip(clip)
        showToast("SQL script copied to clipboard")
    }

    fun copyCleanupSqlToClipboard(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Supabase Cleanup SQL", com.example.data.SupabaseService.CLEANUP_SQL)
        clipboard.setPrimaryClip(clip)
        showToast("Cleanup SQL copied! Paste into Supabase SQL Editor.")
    }

    fun wipeSupabaseCloudData() {
        val config = _uiState.value.supabaseSettings
        if (!config.isConfigured) {
            showToast("Supabase is not configured yet")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val docWipe = com.example.data.SupabaseService.wipeAllDocuments(config)
            val userWipe = com.example.data.SupabaseService.wipeAllUsers(config)

            // Re-register active HR credentials so HR can still log in
            val hrEmail = repository.getHrLoginEmail()
            val hrPass = repository.getHrLoginPassword()
            val hrAccount = com.example.data.UserAccount(
                email = hrEmail,
                password = hrPass,
                role = "HR",
                createdAt = System.currentTimeMillis()
            )
            com.example.data.SupabaseService.registerUser(config, hrAccount)

            _uiState.update { it.copy(isSyncing = false) }
            if (docWipe.isSuccess && userWipe.isSuccess) {
                showToast("All extra data wiped from Supabase! HR account intact.")
            } else {
                showToast("Wiped Supabase app data successfully.")
            }
        }
    }

    fun dismissModal() {
        _uiState.update { it.copy(modalState = ModalState.None) }
    }


    private fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
        viewModelScope.launch {
            delay(2500)
            _uiState.update { if (it.toastMessage == msg) it.copy(toastMessage = null) else it }
        }
    }
}

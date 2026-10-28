package com.example.data

enum class ApiProvider {
    GROQ,
    GEMINI,
    OPENAI_CUSTOM
}

data class ApiKeyItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val provider: ApiProvider,
    val apiKey: String,
    val model: String = "",
    val customUrl: String = "",
    val isExhausted: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
) {
    val maskedKey: String
        get() {
            val trimmed = apiKey.trim()
            return if (trimmed.length > 8) {
                "${trimmed.take(4)}••••${trimmed.takeLast(4)}"
            } else {
                "••••••••"
            }
        }
}

data class DocumentItem(
    val id: String,
    val name: String,
    val chunks: List<String>,
    val uploadedAt: Long,
    val size: Long
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String, // "user" or "bot"
    val text: String = "",
    val isRefusal: Boolean = false,
    val title: String? = null,
    val bullets: List<String>? = null,
    val sourceName: String? = null,
    val sourceText: String? = null,
    val qTerms: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class CompanySettings(
    val orgName: String = "",
    val orgLogoUri: String? = null,
    val hrMail: String = ""
)

data class ApiSettings(
    val keys: List<ApiKeyItem> = emptyList(),
    val legacyApiKey: String = "",
    val legacyApiUrl: String = "https://api.openai.com/v1/chat/completions",
    val legacyModel: String = "gpt-4o-mini"
)

data class PolicyAnswer(
    val title: String = "",
    val bullets: List<String> = emptyList(),
    val sourceName: String = "",
    val sourceText: String = "",
    val qTerms: List<String> = emptyList(),
    val isRefusal: Boolean = false,
    val refusalText: String = ""
)

enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR,
    NEED_TABLE_SETUP
}

data class SupabaseSettings(
    val projectUrl: String = "",
    val anonKey: String = "",
    val isAutoSyncEnabled: Boolean = true,
    val lastSyncTime: Long = 0L,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val statusMessage: String = ""
) {
    val isConfigured: Boolean
        get() = projectUrl.isNotBlank() && anonKey.isNotBlank()

    val maskedKey: String
        get() {
            val trimmed = anonKey.trim()
            return if (trimmed.length > 12) {
                "${trimmed.take(6)}••••••••${trimmed.takeLast(6)}"
            } else if (trimmed.isNotEmpty()) {
                "••••••••"
            } else {
                "Not Configured"
            }
        }
}

data class SupabaseTestResult(
    val success: Boolean,
    val message: String,
    val needsTableSetup: Boolean = false,
    val sqlSnippet: String? = null
)

data class UserAccount(
    val email: String,
    val password: String,
    val role: String = "EMPLOYEE", // "HR" or "EMPLOYEE"
    val createdAt: Long = System.currentTimeMillis(),
    val username: String = ""
)


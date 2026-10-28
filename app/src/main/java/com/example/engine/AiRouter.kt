package com.example.engine

import com.example.BuildConfig
import com.example.data.ApiKeyItem
import com.example.data.ApiProvider
import com.example.data.ApiSettings
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class LlmRewriteResult(
    val title: String,
    val bullets: List<String>
)

object AiRouter {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun isRateLimitOrExhausted(code: Int, body: String): Boolean {
        if (code == 429) return true
        if (code == 403 || code == 400 || code == 401 || code == 503) {
            val lower = body.lowercase()
            return lower.contains("rate") ||
                   lower.contains("quota") ||
                   lower.contains("limit") ||
                   lower.contains("exhaust") ||
                   lower.contains("resource_exhausted") ||
                   lower.contains("insufficient_quota") ||
                   lower.contains("too many requests") ||
                   lower.contains("capacity")
        }
        return false
    }

    suspend fun rewritePolicyAnswer(
        query: String,
        contextPassages: String,
        apiSettings: ApiSettings,
        onKeyExhausted: ((ApiKeyItem) -> Unit)? = null
    ): LlmRewriteResult? = withContext(Dispatchers.IO) {
        // Build the active pool of candidate keys
        val pool = mutableListOf<ApiKeyItem>()

        // 1. User configured keys from settings (non-exhausted first, then any)
        val sortedKeys = apiSettings.keys.sortedBy { if (it.isExhausted) 1 else 0 }
        pool.addAll(sortedKeys)

        // 2. Legacy key if present
        if (apiSettings.legacyApiKey.isNotBlank()) {
            pool.add(
                ApiKeyItem(
                    id = "legacy_openai",
                    provider = ApiProvider.OPENAI_CUSTOM,
                    apiKey = apiSettings.legacyApiKey,
                    customUrl = apiSettings.legacyApiUrl,
                    model = apiSettings.legacyModel
                )
            )
        }

        // 3. BuildConfig Gemini API key if present and not already added
        val buildConfigGemini = try {
            val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
            field.get(null) as? String
        } catch (_: Exception) {
            null
        }

        if (!buildConfigGemini.isNullOrBlank() && buildConfigGemini != "MY_GEMINI_API_KEY") {
            if (pool.none { it.apiKey == buildConfigGemini }) {
                pool.add(
                    ApiKeyItem(
                        id = "build_config_gemini",
                        provider = ApiProvider.GEMINI,
                        apiKey = buildConfigGemini,
                        model = "gemini-3.5-flash"
                    )
                )
            }
        }

        if (pool.isEmpty()) {
            return@withContext null
        }

        val systemPrompt = """
            You are an HR Assistant. Answer using ONLY the CONTEXT below.

            Output format (exactly):
            TITLE: <2-5 word title>
            - <bullet point 1>
            - <bullet point 2>

            Rules:
            1. Every bullet must be a single concise statement directly supported by the context.
            2. Prefer 2-6 bullets. Do not include addresses, phone numbers, page numbers, or boilerplate.
            3. Do not use outside knowledge. Do not guess. Do not invent policies or numbers.
            4. If the answer is not clearly stated in the context, respond with exactly: NOT_FOUND

            CONTEXT:
            $contextPassages
        """.trimIndent()

        // Iterate through keys silently: if current key fails or exhausts limit, immediately shift to next key
        for (item in pool) {
            val key = item.apiKey.trim()
            if (key.isEmpty()) continue

            try {
                val rawResponse = when (item.provider) {
                    ApiProvider.GROQ -> callGroq(key, item.model.ifBlank { "llama-3.3-70b-versatile" }, systemPrompt, query)
                    ApiProvider.GEMINI -> callGemini(key, item.model.ifBlank { "gemini-3.5-flash" }, systemPrompt, query)
                    ApiProvider.OPENAI_CUSTOM -> callOpenAiCustom(
                        key,
                        item.customUrl.ifBlank { "https://api.openai.com/v1/chat/completions" },
                        item.model.ifBlank { "gpt-4o-mini" },
                        systemPrompt,
                        query
                    )
                }

                if (rawResponse != null) {
                    val parsed = parseLlmOutput(rawResponse)
                    if (parsed != null) {
                        return@withContext parsed
                    }
                    if (rawResponse.contains("NOT_FOUND", ignoreCase = true) && rawResponse.length < 80) {
                        return@withContext null // Definite refusal from LLM
                    }
                }
            } catch (e: RateLimitExhaustedException) {
                // Key exhausted or rate-limited: silently shift to next key
                onKeyExhausted?.invoke(item)
                continue
            } catch (e: Exception) {
                // Any connection or transient failure: silently continue to next key
                continue
            }
        }

        // If all keys fail, returns null so local extraction is used silently
        null
    }

    private class RateLimitExhaustedException(message: String) : IOException(message)

    private fun callGroq(apiKey: String, model: String, systemPrompt: String, userQuery: String): String? {
        val json = JSONObject().apply {
            put("model", model)
            put("temperature", 0.1)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userQuery)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                if (isRateLimitOrExhausted(response.code, body)) {
                    throw RateLimitExhaustedException("Groq rate limit: ${response.code}")
                }
                return null
            }
            val root = JSONObject(body)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val firstChoice = choices.getJSONObject(0)
            val msg = firstChoice.optJSONObject("message") ?: return null
            return msg.optString("content", "").trim()
        }
    }

    private fun callGemini(apiKey: String, model: String, systemPrompt: String, userQuery: String): String? {
        val targetModel = if (model.isBlank()) "gemini-3.5-flash" else model
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$apiKey"

        val json = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", userQuery) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                if (isRateLimitOrExhausted(response.code, body)) {
                    throw RateLimitExhaustedException("Gemini rate limit: ${response.code}")
                }
                return null
            }
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            val firstPart = parts.getJSONObject(0)
            return firstPart.optString("text", "").trim()
        }
    }

    private fun callOpenAiCustom(apiKey: String, endpoint: String, model: String, systemPrompt: String, userQuery: String): String? {
        val json = JSONObject().apply {
            put("model", model)
            put("temperature", 0.1)
            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", userQuery)
                })
            }
            put("messages", messages)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(json.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                if (isRateLimitOrExhausted(response.code, body)) {
                    throw RateLimitExhaustedException("OpenAI Custom rate limit: ${response.code}")
                }
                return null
            }
            val root = JSONObject(body)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val firstChoice = choices.getJSONObject(0)
            val msg = firstChoice.optJSONObject("message") ?: return null
            return msg.optString("content", "").trim()
        }
    }

    private fun parseLlmOutput(raw: String): LlmRewriteResult? {
        val clean = raw.trim()
        if (clean.isEmpty()) return null
        if (clean.contains("NOT_FOUND", ignoreCase = true) && clean.length < 80) return null

        var title = ""
        val bullets = mutableListOf<String>()

        for (line in clean.split("\n").map { it.trim() }.filter { it.isNotEmpty() }) {
            val titleMatch = Regex("^TITLE:\\s*(.+)$", RegexOption.IGNORE_CASE).find(line)
            if (titleMatch != null) {
                title = titleMatch.groupValues[1].trim()
                continue
            }
            val bulletMatch = Regex("^[-•*]\\s+(.+)$").find(line)
            if (bulletMatch != null) {
                bullets.add(bulletMatch.groupValues[1].trim())
                continue
            }
            if (title.isEmpty() && bullets.isEmpty()) {
                title = line
            } else if (bullets.isNotEmpty()) {
                val lastIdx = bullets.size - 1
                bullets[lastIdx] = bullets[lastIdx] + " " + line
            }
        }

        return if (bullets.isNotEmpty()) {
            LlmRewriteResult(
                title = if (title.isNotBlank()) title else "Policy Details",
                bullets = bullets
            )
        } else null
    }
}

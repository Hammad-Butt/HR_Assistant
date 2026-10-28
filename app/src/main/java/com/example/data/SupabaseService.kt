package com.example.data

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

object SupabaseService {

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    const val TABLE_SQL = """-- Run this once in your Supabase SQL Editor:
create table if not exists public.company_documents (
  id text primary key,
  name text not null,
  chunks jsonb not null default '[]'::jsonb,
  uploaded_at bigint not null,
  size bigint not null default 0
);

create table if not exists public.company_users (
  email text primary key,
  password text not null,
  role text not null default 'EMPLOYEE',
  created_at bigint not null,
  username text default ''
);
alter table public.company_users add column if not exists username text default '';

-- Enable Row Level Security (RLS)
alter table public.company_documents enable row level security;
alter table public.company_users enable row level security;

-- Allow read & write access with public anon API key
create policy "Allow public read docs" on public.company_documents for select using (true);
create policy "Allow public insert docs" on public.company_documents for insert with check (true);
create policy "Allow public update docs" on public.company_documents for update using (true);
create policy "Allow public delete docs" on public.company_documents for delete using (true);

create policy "Allow public read users" on public.company_users for select using (true);
create policy "Allow public insert users" on public.company_users for insert with check (true);
create policy "Allow public update users" on public.company_users for update using (true);
create policy "Allow public delete users" on public.company_users for delete using (true);"""

    const val CLEANUP_SQL = """-- Run this in your Supabase SQL Editor to delete ALL extra/unrelated tables and keep ONLY this app's tables:
DO $$ 
DECLARE 
    r RECORD;
BEGIN
    FOR r IN (
        SELECT tablename 
        FROM pg_tables 
        WHERE schemaname = 'public' 
          AND tablename NOT IN ('company_documents', 'company_users')
    ) LOOP
        EXECUTE 'DROP TABLE IF EXISTS public.' || quote_ident(r.tablename) || ' CASCADE;';
    END LOOP;
END $$;

-- Ensure required app tables exist cleanly:
create table if not exists public.company_documents (
  id text primary key,
  name text not null,
  chunks jsonb not null default '[]'::jsonb,
  uploaded_at bigint not null,
  size bigint not null default 0
);

create table if not exists public.company_users (
  email text primary key,
  password text not null,
  role text not null default 'EMPLOYEE',
  created_at bigint not null,
  username text default ''
);
alter table public.company_users add column if not exists username text default '';

-- Enable RLS
alter table public.company_documents enable row level security;
alter table public.company_users enable row level security;

-- Drop and recreate public policies
drop policy if exists "Allow public read docs" on public.company_documents;
drop policy if exists "Allow public insert docs" on public.company_documents;
drop policy if exists "Allow public update docs" on public.company_documents;
drop policy if exists "Allow public delete docs" on public.company_documents;

create policy "Allow public read docs" on public.company_documents for select using (true);
create policy "Allow public insert docs" on public.company_documents for insert with check (true);
create policy "Allow public update docs" on public.company_documents for update using (true);
create policy "Allow public delete docs" on public.company_documents for delete using (true);

drop policy if exists "Allow public read users" on public.company_users;
drop policy if exists "Allow public insert users" on public.company_users;
drop policy if exists "Allow public update users" on public.company_users;
drop policy if exists "Allow public delete users" on public.company_users;

create policy "Allow public read users" on public.company_users for select using (true);
create policy "Allow public insert users" on public.company_users for insert with check (true);
create policy "Allow public update users" on public.company_users for update using (true);
create policy "Allow public delete users" on public.company_users for delete using (true);"""

    private fun normalizeUrl(url: String): String {
        return url.trim().removeSuffix("/").removeSuffix("/rest/v1")
    }

    private fun isMissingTable(code: Int, body: String): Boolean {
        if (code == 404) return true
        val lower = body.lowercase()
        return lower.contains("42p01") ||
               lower.contains("relation") && lower.contains("does not exist") ||
               lower.contains("could not find the table")
    }

    suspend fun testConnection(settings: SupabaseSettings): SupabaseTestResult = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()

        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext SupabaseTestResult(
                success = false,
                message = "Project URL and Anon Key must not be empty."
            )
        }

        val testUrl = "$base/rest/v1/company_documents?select=id&limit=1"
        val request = Request.Builder()
            .url(testUrl)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    return@withContext SupabaseTestResult(
                        success = true,
                        message = "Connected to Supabase! Table `company_documents` is ready."
                    )
                } else if (isMissingTable(response.code, body)) {
                    return@withContext SupabaseTestResult(
                        success = false,
                        message = "Connected to Supabase, but table `company_documents` does not exist yet. Please run the SQL setup script.",
                        needsTableSetup = true,
                        sqlSnippet = TABLE_SQL
                    )
                } else {
                    return@withContext SupabaseTestResult(
                        success = false,
                        message = "Supabase error HTTP ${response.code}: $body"
                    )
                }
            }
        } catch (e: Exception) {
            return@withContext SupabaseTestResult(
                success = false,
                message = "Connection failed: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    suspend fun fetchAllDocuments(settings: SupabaseSettings): Result<List<DocumentItem>> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()

        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
        }

        val endpoint = "$base/rest/v1/company_documents?select=*&order=uploaded_at.desc"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .get()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (isMissingTable(response.code, body)) {
                        return@withContext Result.failure(MissingTableException("Table `company_documents` does not exist in Supabase yet."))
                    }
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $body"))
                }

                val list = mutableListOf<DocumentItem>()
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id", "")
                    val name = obj.optString("name", "Document")
                    val uploadedAt = obj.optLong("uploaded_at", obj.optLong("uploadedAt", System.currentTimeMillis()))
                    val size = obj.optLong("size", 0L)

                    val chunksList = mutableListOf<String>()
                    val chunksObj = obj.opt("chunks")
                    if (chunksObj is JSONArray) {
                        for (c in 0 until chunksObj.length()) {
                            chunksList.add(chunksObj.getString(c))
                        }
                    } else if (chunksObj is String && chunksObj.isNotBlank()) {
                        try {
                            val parsedArr = JSONArray(chunksObj)
                            for (c in 0 until parsedArr.length()) {
                                chunksList.add(parsedArr.getString(c))
                            }
                        } catch (_: Exception) {
                            chunksList.add(chunksObj)
                        }
                    }

                    if (id.isNotBlank() && chunksList.isNotEmpty()) {
                        list.add(
                            DocumentItem(
                                id = id,
                                name = name,
                                chunks = chunksList,
                                uploadedAt = uploadedAt,
                                size = size
                            )
                        )
                    }
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun upsertDocument(settings: SupabaseSettings, doc: DocumentItem): Result<Unit> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()

        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
        }

        val endpoint = "$base/rest/v1/company_documents"
        val payload = JSONObject().apply {
            put("id", doc.id)
            put("name", doc.name)
            val chunksArr = JSONArray()
            doc.chunks.forEach { chunksArr.put(it) }
            put("chunks", chunksArr)
            put("uploaded_at", doc.uploadedAt)
            put("size", doc.size)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    if (isMissingTable(response.code, body)) {
                        Result.failure(MissingTableException("Table `company_documents` does not exist in Supabase yet."))
                    } else {
                        Result.failure(IOException("Failed to upload document (HTTP ${response.code}): $body"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(settings: SupabaseSettings, docId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()

        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
        }

        val endpoint = "$base/rest/v1/company_documents?id=eq.$docId"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .delete()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IOException("Failed to delete document (HTTP ${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun wipeAllDocuments(settings: SupabaseSettings): Result<Unit> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()

        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
        }

        val endpoint = "$base/rest/v1/company_documents?id=neq.placeholder_keep_none"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .delete()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IOException("Failed to wipe documents (HTTP ${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchUser(settings: SupabaseSettings, email: String): Result<UserAccount?> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()
        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Supabase not configured"))
        }
        val encodedEmail = java.net.URLEncoder.encode(email.trim().lowercase(), "UTF-8")
        val endpoint = "$base/rest/v1/company_users?email=eq.$encodedEmail&select=*"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .get()
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    if (isMissingTable(response.code, body)) {
                        return@withContext Result.failure(MissingTableException("Table `company_users` does not exist."))
                    }
                    return@withContext Result.failure(IOException("HTTP ${response.code}: $body"))
                }
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val user = UserAccount(
                        email = obj.optString("email", email),
                        password = obj.optString("password", ""),
                        role = obj.optString("role", "EMPLOYEE"),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        username = obj.optString("username", "")
                    )
                    Result.success(user)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(settings: SupabaseSettings, account: UserAccount): Result<Unit> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()
        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Supabase not configured"))
        }
        val encodedEmail = java.net.URLEncoder.encode(account.email.trim().lowercase(), "UTF-8")
        val endpoint = "$base/rest/v1/company_users?on_conflict=email"
        val payload = JSONObject().apply {
            put("email", account.email.trim().lowercase())
            put("password", account.password)
            put("role", account.role)
            put("created_at", account.createdAt)
            if (account.username.isNotEmpty()) {
                put("username", account.username)
            }
        }
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=minimal")
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    return@withContext Result.success(Unit)
                }

                // If conflict (409) or bad request due to merge-duplicates, try PATCH to update existing record
                if (response.code == 409 || (response.code == 400 && body.contains("conflict", ignoreCase = true))) {
                    try {
                        val patchUrl = "$base/rest/v1/company_users?email=eq.$encodedEmail"
                        val patchRequest = Request.Builder()
                            .url(patchUrl)
                            .addHeader("apikey", anon)
                            .addHeader("Authorization", "Bearer $anon")
                            .addHeader("Content-Type", "application/json")
                            .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                            .build()
                        httpClient.newCall(patchRequest).execute().use { patchResp ->
                            if (patchResp.isSuccessful) {
                                return@withContext Result.success(Unit)
                            }
                        }
                    } catch (_: Exception) {}
                } else if (response.code == 400) {
                    // Try standard POST without merge-duplicates
                    try {
                        val plainRequest = Request.Builder()
                            .url("$base/rest/v1/company_users")
                            .addHeader("apikey", anon)
                            .addHeader("Authorization", "Bearer $anon")
                            .addHeader("Content-Type", "application/json")
                            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
                            .build()
                        httpClient.newCall(plainRequest).execute().use { plainResp ->
                            if (plainResp.isSuccessful) {
                                return@withContext Result.success(Unit)
                            }
                        }
                    } catch (_: Exception) {}
                }

                if (isMissingTable(response.code, body)) {
                    return@withContext Result.failure(MissingTableException("Table `company_users` does not exist in Supabase yet."))
                }
                Result.failure(IOException("Database error (HTTP ${response.code}): $body"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(settings: SupabaseSettings, email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()
        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
        }
        val encodedEmail = java.net.URLEncoder.encode(email.trim().lowercase(), "UTF-8")
        val endpoint = "$base/rest/v1/company_users?email=eq.$encodedEmail"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .delete()
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IOException("Failed to delete user (HTTP ${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun wipeAllUsers(settings: SupabaseSettings): Result<Unit> = withContext(Dispatchers.IO) {
        val base = normalizeUrl(settings.projectUrl)
        val anon = settings.anonKey.trim()

        if (base.isEmpty() || anon.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("Supabase credentials not configured."))
        }

        val endpoint = "$base/rest/v1/company_users?email=neq.placeholder_keep_none"
        val request = Request.Builder()
            .url(endpoint)
            .addHeader("apikey", anon)
            .addHeader("Authorization", "Bearer $anon")
            .delete()
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    val body = response.body?.string().orEmpty()
                    Result.failure(IOException("Failed to wipe users (HTTP ${response.code}): $body"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class MissingTableException(message: String) : Exception(message)

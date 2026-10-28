package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ApiKeyItem
import com.example.data.ApiProvider
import com.example.data.ApiSettings
import com.example.data.HRRepository
import com.example.engine.PolicyEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HR Assistant", appName)
    }

    @Test
    fun `greeting detection works as expected`() {
        // Test variations of greetings without API calls
        val greetings = listOf("hello", "hy", "yo", "yo bro", "wassup", "heyyy", "hi there", "hy hr")
        for (g in greetings) {
            val response = PolicyEngine.detectGreeting(g)
            assertNotNull("Greeting '$g' should be detected", response)
            assertTrue(response!!.contains("HR Assistant"))
        }

        val howAreYouResponse = PolicyEngine.detectGreeting("how are you?")
        assertNotNull(howAreYouResponse)
        assertTrue(howAreYouResponse!!.contains("ready to help"))
    }

    @Test
    fun `policy query returns matching bullets`() {
        val doc = PolicyEngine.getDefaultHandbookDocument()
        val answer = PolicyEngine.answerQuery("What is the annual leave policy?", listOf(doc))
        assertFalse(answer.isRefusal)
        assertEquals("Annual Leave Policy", answer.title)
        assertTrue(answer.bullets.isNotEmpty())
        assertTrue(answer.bullets.any { it.contains("20 days", ignoreCase = true) })
    }

    @Test
    fun `apiKeyItem masking and repository persistence works`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HRRepository(context)

        val groqKey = ApiKeyItem(
            provider = ApiProvider.GROQ,
            apiKey = "gsk_1234567890abcdef",
            model = "llama-3.3-70b-versatile"
        )
        val geminiKey = ApiKeyItem(
            provider = ApiProvider.GEMINI,
            apiKey = "AIzaSyDummyGeminiKey99",
            model = "gemini-3.5-flash"
        )

        assertEquals("gsk_••••cdef", groqKey.maskedKey)
        assertEquals("AIza••••ey99", geminiKey.maskedKey)

        val settings = ApiSettings(
            keys = listOf(groqKey, geminiKey)
        )
        repository.saveApiSettings(settings)

        val loaded = repository.loadApiSettings()
        assertEquals(2, loaded.keys.size)
        assertEquals(ApiProvider.GROQ, loaded.keys[0].provider)
        assertEquals("gsk_1234567890abcdef", loaded.keys[0].apiKey)
        assertEquals(ApiProvider.GEMINI, loaded.keys[1].provider)
        assertEquals("AIzaSyDummyGeminiKey99", loaded.keys[1].apiKey)
    }

    @Test
    fun `theme persistence toggling works correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = HRRepository(context)

        repository.setIsDarkTheme(false)
        assertFalse(repository.getIsDarkTheme())

        repository.setIsDarkTheme(true)
        assertTrue(repository.getIsDarkTheme())
    }
}

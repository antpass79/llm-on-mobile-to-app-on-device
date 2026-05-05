package com.esaote.imageparams.network

import android.util.Log
import com.esaote.imageparams.model.ParameterPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class GeminiClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val apiKey: String,
    private val model: String = "gemini-2.0-flash",
) : LlmClient {
    companion object {
        private const val TAG = "GeminiClient"
        private const val MAX_RETRIES = 3
        private const val RETRY_BASE_DELAY_MS = 5_000L
    }

    private val systemPrompt = """
        You map ultrasound image parameter voice commands into JSON.
        Valid keys: gain (0-100), depth (0-300), zoom (1-10).
        Return only a JSON object without markdown or extra text.
        Example: {"gain":65,"zoom":2}
        If the command has no supported parameter, return {}.
    """.trimIndent()

    override suspend fun parseVoiceCommand(transcript: String): Result<ParameterPayload> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key is not configured"))
        }

        return withContext(Dispatchers.IO) {
            var lastError: Throwable = IllegalStateException("Unknown error")
            repeat(MAX_RETRIES) { attempt ->
                val result = runCatching { callGemini(transcript) }
                if (result.isSuccess) return@withContext result
                lastError = result.exceptionOrNull()!!
                val isRateLimit = lastError.message?.contains("429") == true
                if (!isRateLimit || attempt == MAX_RETRIES - 1) return@withContext result
                val delayMs = RETRY_BASE_DELAY_MS * (1 shl attempt) // 5s, 10s, 20s
                Log.w(TAG, "429 rate limit — retrying in ${delayMs}ms (attempt ${attempt + 1}/$MAX_RETRIES)")
                delay(delayMs)
            }
            Result.failure(lastError)
        }
    }

    private fun callGemini(transcript: String): ParameterPayload {
        Log.i(TAG, "callGemini: \"$transcript\"")
        val requestBody = GenerateContentRequest(
            systemInstruction = Content(parts = listOf(Part(systemPrompt))),
            contents = listOf(Content(parts = listOf(Part(transcript)), role = "user")),
            generationConfig = GenerationConfig(
                temperature = 0.0,
                responseMimeType = "application/json",
            ),
        )

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        val responseText = httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Gemini request failed: ${response.code}")
            }
            response.body?.string().orEmpty()
        }

                val response = json.decodeFromString<GenerateContentResponse>(responseText)
                val content = response.candidates
                    .firstOrNull()?.content?.parts?.firstOrNull()?.text
                    .orEmpty().trim()

        Log.i(TAG, "Gemini response content: \"$content\"")
        return if (content.isBlank()) ParameterPayload() else json.decodeFromString(content)
    }
}

@Serializable
private data class GenerateContentRequest(
    val systemInstruction: Content,
    val contents: List<Content>,
    val generationConfig: GenerationConfig,
)

@Serializable
private data class Content(
    val parts: List<Part>,
    val role: String? = null,
)

@Serializable
private data class Part(val text: String)

@Serializable
private data class GenerationConfig(
    val temperature: Double,
    val responseMimeType: String,
)

@Serializable
private data class GenerateContentResponse(
    val candidates: List<Candidate>,
)

@Serializable
private data class Candidate(
    val content: Content,
)

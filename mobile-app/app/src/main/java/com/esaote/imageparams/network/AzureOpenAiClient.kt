package com.esaote.imageparams.network

import com.esaote.imageparams.model.ParameterPayload
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AzureOpenAiClient(
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val endpoint: String,
    private val apiKey: String,
    private val deployment: String,
) {
    suspend fun parseVoiceCommand(transcript: String): Result<ParameterPayload> {
        if (endpoint.isBlank() || apiKey.isBlank() || deployment.isBlank()) {
            return Result.failure(IllegalStateException("LLM configuration is incomplete"))
        }

        return runCatching {
            val systemPrompt = """
                You map ultrasound image parameter voice commands into JSON.
                Valid keys: gain (0-100), depth (0-300), zoom (1-10).
                Return only a JSON object without markdown or extra text.
                Example: {\"gain\":65,\"zoom\":2}
                If command has no supported parameter, return {}.
            """.trimIndent()

            val requestBody = ChatCompletionsRequest(
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = transcript),
                ),
                temperature = 0.0,
            )

            val url = endpoint.trimEnd('/') +
                "/openai/deployments/$deployment/chat/completions?api-version=2024-08-01-preview"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("api-key", apiKey)
                .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
                .build()

            val responseText = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("LLM request failed: ${response.code}")
                }
                response.body?.string().orEmpty()
            }

            val response = json.decodeFromString<ChatCompletionsResponse>(responseText)
            val content = response.choices.firstOrNull()?.message?.content.orEmpty().trim()
            if (content.isBlank()) {
                ParameterPayload()
            } else {
                json.decodeFromString<ParameterPayload>(content)
            }
        }
    }
}

@Serializable
private data class ChatCompletionsRequest(
    val messages: List<ChatMessage>,
    val temperature: Double,
)

@Serializable
private data class ChatMessage(
    val role: String,
    val content: String,
)

@Serializable
private data class ChatCompletionsResponse(
    val choices: List<ChatChoice>,
)

@Serializable
private data class ChatChoice(
    val message: ChatMessage,
)

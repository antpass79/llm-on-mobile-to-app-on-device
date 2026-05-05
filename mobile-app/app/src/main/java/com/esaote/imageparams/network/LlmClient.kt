package com.esaote.imageparams.network

import com.esaote.imageparams.model.ParameterPayload

/**
 * Common interface for LLM backends that parse voice transcripts
 * into image parameter payloads.
 */
interface LlmClient {
    suspend fun parseVoiceCommand(transcript: String): Result<ParameterPayload>
}

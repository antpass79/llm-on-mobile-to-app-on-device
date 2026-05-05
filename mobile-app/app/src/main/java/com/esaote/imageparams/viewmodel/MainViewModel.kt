package com.esaote.imageparams.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esaote.imageparams.model.ImageParameters
import com.esaote.imageparams.model.ParameterPayload
import com.esaote.imageparams.network.LlmClient
import com.esaote.imageparams.network.WebSocketParameterClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val socketClient: WebSocketParameterClient,
    private val llmClient: LlmClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            socketClient.isConnected.collect { connected ->
                _uiState.update { it.copy(isConnected = connected) }
            }
        }

        socketClient.onParametersReceived = { payload ->
            _uiState.update { current ->
                current.copy(
                    parameters = current.parameters.copy(
                        gain = payload.gain ?: current.parameters.gain,
                        depth = payload.depth ?: current.parameters.depth,
                        zoom = payload.zoom ?: current.parameters.zoom,
                    )
                )
            }
        }
    }

    fun connect(url: String) = socketClient.connect(url)
    fun disconnect() = socketClient.disconnect()

    /** Toggle continuous microphone on/off. */
    fun toggleMic() {
        _uiState.update { it.copy(isMicEnabled = !it.isMicEnabled, liveTranscript = "") }
    }

    /** Force the mic to a specific enabled state (e.g. after permission denial). */
    fun setMicEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isMicEnabled = enabled, liveTranscript = "") }
    }

    /** Called with live partial speech while the user is still speaking. */
    fun onPartialTranscript(text: String) {
        _uiState.update { it.copy(liveTranscript = text) }
    }

    /** Called with the final transcript after a speech pause — forwards to Gemini. */
    fun onFinalTranscript(transcript: String) {
        _uiState.update {
            it.copy(
                liveTranscript = "",
                lastTranscript = transcript,
                statusMessage = "Calling Gemini...",
            )
        }

        viewModelScope.launch {
            llmClient.parseVoiceCommand(transcript)
                .onSuccess { payload ->
                    updateAndSend(payload)
                    _uiState.update { it.copy(statusMessage = "Applied") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(statusMessage = "LLM error: ${error.message}") }
                }
        }
    }

    fun updateGain(gain: Int) = updateAndSend(ParameterPayload(gain = gain))
    fun updateDepth(depth: Int) = updateAndSend(ParameterPayload(depth = depth))
    fun updateZoom(zoom: Int) = updateAndSend(ParameterPayload(zoom = zoom))

    private fun updateAndSend(payload: ParameterPayload) {
        _uiState.update { current ->
            current.copy(
                parameters = current.parameters.copy(
                    gain = payload.gain ?: current.parameters.gain,
                    depth = payload.depth ?: current.parameters.depth,
                    zoom = payload.zoom ?: current.parameters.zoom,
                )
            )
        }
        socketClient.sendUpdate(payload)
    }

    override fun onCleared() {
        socketClient.disconnect()
        super.onCleared()
    }
}

data class UiState(
    val parameters: ImageParameters = ImageParameters(),
    val isConnected: Boolean = false,
    val statusMessage: String = "Idle",
    /** Final transcript last sent to Gemini. */
    val lastTranscript: String = "",
    /** Partial (live) transcript while the user is speaking. */
    val liveTranscript: String = "",
    val isMicEnabled: Boolean = false,
)


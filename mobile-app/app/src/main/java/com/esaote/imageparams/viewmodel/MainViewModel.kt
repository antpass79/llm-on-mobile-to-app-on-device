package com.esaote.imageparams.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.esaote.imageparams.model.ImageParameters
import com.esaote.imageparams.model.ParameterPayload
import com.esaote.imageparams.network.AzureOpenAiClient
import com.esaote.imageparams.network.WebSocketParameterClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val socketClient: WebSocketParameterClient,
    private val llmClient: AzureOpenAiClient,
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

    fun connect(url: String) {
        socketClient.connect(url)
    }

    fun disconnect() {
        socketClient.disconnect()
    }

    fun updateGain(gain: Int) {
        updateAndSend(ParameterPayload(gain = gain))
    }

    fun updateDepth(depth: Int) {
        updateAndSend(ParameterPayload(depth = depth))
    }

    fun updateZoom(zoom: Int) {
        updateAndSend(ParameterPayload(zoom = zoom))
    }

    fun processVoiceTranscript(transcript: String) {
        _uiState.update { it.copy(lastTranscript = transcript, statusMessage = "Calling LLM...") }

        viewModelScope.launch {
            llmClient.parseVoiceCommand(transcript)
                .onSuccess { payload ->
                    updateAndSend(payload)
                    _uiState.update { it.copy(statusMessage = "Voice command applied") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(statusMessage = "LLM error: ${error.message}") }
                }
        }
    }

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
    val lastTranscript: String = "",
)

package com.esaote.imageparams

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.esaote.imageparams.config.AppConfig
import com.esaote.imageparams.network.AzureOpenAiClient
import com.esaote.imageparams.network.WebSocketParameterClient
import com.esaote.imageparams.speech.VoiceInputManager
import com.esaote.imageparams.ui.MainScreen
import com.esaote.imageparams.viewmodel.MainViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var viewModel: MainViewModel

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                voiceInputManager.setEnabled(true)
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                viewModel.setMicEnabled(false)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val json = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

        val client = OkHttpClient.Builder().build()
        val socketClient = WebSocketParameterClient(client, json)
        val llmClient = AzureOpenAiClient(
            httpClient = client,
            json = json,
            endpoint = AppConfig.azureUrl,
            apiKey = AppConfig.azureKey,
            deployment = AppConfig.azureModel,
        )

        voiceInputManager = VoiceInputManager(
            context = this,
            onPartialResult = { text -> viewModel.onPartialTranscript(text) },
            onFinalResult = { transcript -> viewModel.onFinalTranscript(transcript) },
            onError = { error -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show() },
        )

        viewModel = MainViewModel(socketClient, llmClient)

        // React to mic toggle: request permission if needed, then enable/disable the manager.
        lifecycleScope.launch {
            viewModel.uiState
                .map { it.isMicEnabled }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) requestMicrophoneAndStart() else voiceInputManager.setEnabled(false)
                }
        }

        setContent {
            MainScreen(viewModel = viewModel, defaultWsUrl = AppConfig.wsUrl)
        }
    }

    private fun requestMicrophoneAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            voiceInputManager.setEnabled(true)
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onDestroy() {
        voiceInputManager.release()
        super.onDestroy()
    }
}


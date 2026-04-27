package com.esaote.imageparams

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.esaote.imageparams.config.AppConfig
import com.esaote.imageparams.network.AzureOpenAiClient
import com.esaote.imageparams.network.WebSocketParameterClient
import com.esaote.imageparams.speech.VoiceInputManager
import com.esaote.imageparams.ui.MainScreen
import com.esaote.imageparams.viewmodel.MainViewModel
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var viewModel: MainViewModel

    private val microphonePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startVoiceCapture()
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
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
            endpoint = AppConfig.llmUrl,
            apiKey = AppConfig.llmKey,
            deployment = AppConfig.llmModel,
        )

        voiceInputManager = VoiceInputManager(this)
        viewModel = MainViewModel(socketClient, llmClient)

        setContent {
            MainScreen(
                viewModel = viewModel,
                defaultWsUrl = AppConfig.wsUrl,
                onVoiceClick = { requestVoiceInput() },
            )
        }
    }

    private fun requestVoiceInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceCapture()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceCapture() {
        voiceInputManager.listen(
            onResult = { transcript -> viewModel.processVoiceTranscript(transcript) },
            onError = { error -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show() },
        )
    }

    override fun onDestroy() {
        voiceInputManager.release()
        super.onDestroy()
    }
}

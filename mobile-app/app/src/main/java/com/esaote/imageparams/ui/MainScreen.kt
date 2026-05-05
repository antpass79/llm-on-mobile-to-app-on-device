package com.esaote.imageparams.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esaote.imageparams.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    defaultWsUrl: String,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var wsUrl by rememberSaveable { mutableStateOf(defaultWsUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Image Parameter Controller",
            style = MaterialTheme.typography.headlineSmall,
        )

        // — Connection ——————————————————————————————————————
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = wsUrl,
            onValueChange = { wsUrl = it },
            label = { Text("WebSocket URL") },
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.connect(wsUrl) }) { Text("Connect") }
            Button(onClick = { viewModel.disconnect() }) { Text("Disconnect") }
        }

        Text(if (uiState.isConnected) "Socket: connected" else "Socket: disconnected")

        HorizontalDivider()

        // — Microphone ——————————————————————————————————————
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Microphone", style = MaterialTheme.typography.titleMedium)
            FilledIconToggleButton(
                checked = uiState.isMicEnabled,
                onCheckedChange = { viewModel.toggleMic() },
            ) {
                Icon(
                    imageVector = if (uiState.isMicEnabled) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = if (uiState.isMicEnabled) "Disable microphone" else "Enable microphone",
                )
            }
        }

        // — Transcript ——————————————————————————————————————
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("Transcript", style = MaterialTheme.typography.labelMedium)

                val display = uiState.liveTranscript.ifBlank { uiState.lastTranscript }
                Text(
                    text = display.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (uiState.liveTranscript.isNotBlank())
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (uiState.statusMessage != "Idle") {
                    Text(
                        text = uiState.statusMessage,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        HorizontalDivider()

        // — Parameters ——————————————————————————————————————
        ParameterSlider("Gain", uiState.parameters.gain, 0f, 100f) { viewModel.updateGain(it) }
        ParameterSlider("Depth", uiState.parameters.depth, 0f, 300f) { viewModel.updateDepth(it) }
        ParameterSlider("Zoom", uiState.parameters.zoom, 1f, 10f) { viewModel.updateZoom(it) }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Voice examples: \"set gain to 70\", \"increase depth to 140\", \"zoom 2\".",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ParameterSlider(
    title: String,
    value: Int,
    min: Float,
    max: Float,
    onValueChange: (Int) -> Unit,
) {
    var localValue by remember(value) { mutableStateOf(value.toFloat()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$title: $value")
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChange(localValue.toInt()) },
            valueRange = min..max,
        )
    }
}


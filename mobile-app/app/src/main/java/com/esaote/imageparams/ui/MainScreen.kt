package com.esaote.imageparams.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.esaote.imageparams.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    defaultWsUrl: String,
    onVoiceClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var wsUrl by rememberSaveable { mutableStateOf(defaultWsUrl) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Image Parameter Controller",
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = wsUrl,
            onValueChange = { wsUrl = it },
            label = { Text("WebSocket URL") },
            singleLine = true,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.connect(wsUrl) }) {
                Text("Connect")
            }
            Button(onClick = { viewModel.disconnect() }) {
                Text("Disconnect")
            }
            Button(onClick = onVoiceClick) {
                Text("Voice")
            }
        }

        Text(if (uiState.isConnected) "Socket: connected" else "Socket: disconnected")
        Text("Status: ${uiState.statusMessage}")
        if (uiState.lastTranscript.isNotBlank()) {
            Text("Last voice: ${uiState.lastTranscript}")
        }

        ParameterSliderInt(
            title = "Gain",
            value = uiState.parameters.gain,
            min = 0f,
            max = 100f,
            onValueChange = { viewModel.updateGain(it) }
        )

        ParameterSliderInt(
            title = "Depth",
            value = uiState.parameters.depth,
            min = 0f,
            max = 300f,
            onValueChange = { viewModel.updateDepth(it) }
        )

        ParameterSliderDouble(
            title = "Zoom",
            value = uiState.parameters.zoom,
            min = 1f,
            max = 10f,
            step = 0.5f,
            onValueChange = { viewModel.updateZoom(it) }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("Voice examples: 'set gain to 70', 'increase depth to 140', 'zoom 2'.")
    }
}

@Composable
private fun ParameterSliderInt(
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

@Composable
private fun ParameterSliderDouble(
    title: String,
    value: Double,
    min: Float,
    max: Float,
    step: Float,
    onValueChange: (Double) -> Unit,
) {
    var localValue by remember(value) { mutableStateOf(value.toFloat()) }
    val steps = (((max - min) / step).toInt() - 1).coerceAtLeast(0)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$title: %.1f".format(value))
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = localValue,
            onValueChange = { localValue = it },
            onValueChangeFinished = { onValueChange(localValue.toDouble()) },
            valueRange = min..max,
            steps = steps,
        )
    }
}

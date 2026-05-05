package com.esaote.imageparams.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

/**
 * Continuous-listening voice manager.
 *
 * Call [setEnabled] to start/stop listening. While enabled the manager
 * automatically restarts after each utterance (Android SpeechRecognizer
 * already handles the in-utterance pause that triggers the final result).
 *
 * All public methods must be called from the main thread (same requirement
 * as SpeechRecognizer itself).
 */
class VoiceInputManager(
    private val context: Context,
    private val onPartialResult: (String) -> Unit,
    private val onFinalResult: (String) -> Unit,
    private val onError: (String) -> Unit,
) {
    // Recreated on every session start to guarantee a clean state.
    private var recognizer: SpeechRecognizer? = null

    private val handler = Handler(Looper.getMainLooper())
    private var isEnabled = false
    private var isListening = false

    // Errors that are normal during continuous-listen loops — don't surface as toasts.
    private val silentErrors = setOf(
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        SpeechRecognizer.ERROR_CLIENT,
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY,
    )

    private val restartDelayMs = 500L

    companion object { private const val TAG = "VoiceInputManager" }

    private fun buildListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { Log.i(TAG, "onReadyForSpeech") }
        override fun onBeginningOfSpeech() { Log.i(TAG, "onBeginningOfSpeech") }
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() { Log.i(TAG, "onEndOfSpeech") }
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            Log.i(TAG, "onPartialResults: $text")
            if (!text.isNullOrBlank()) onPartialResult(text)
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            Log.i(TAG, "onResults: \"$text\"")
            if (text.isNotBlank()) onFinalResult(text)
            if (isEnabled) scheduleRestart()
        }

        override fun onError(error: Int) {
            isListening = false
            Log.w(TAG, "onError: $error (silent=${error in silentErrors})")
            if (error !in silentErrors) {
                onError("Speech recognition error: $error")
            }
            if (isEnabled) scheduleRestart()
        }
    }

    /** Enable or disable continuous listening. */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        handler.removeCallbacksAndMessages(null)
        if (enabled) {
            startListening()
        } else {
            isListening = false
            destroyRecognizer()
        }
    }

    private fun scheduleRestart() {
        handler.postDelayed({ if (isEnabled) startListening() }, restartDelayMs)
    }

    private fun startListening() {
        if (isListening) return
        // Destroy any previous instance — this is the only reliable way to avoid
        // stale onError(5)/onError(7) callbacks from a prior session racing into
        // a freshly-started one and triggering an infinite restart loop.
        destroyRecognizer()
        isListening = true
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also { sr ->
            sr.setRecognitionListener(buildListener())
            sr.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
            )
        }
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    fun release() {
        isEnabled = false
        handler.removeCallbacksAndMessages(null)
        destroyRecognizer()
    }
}

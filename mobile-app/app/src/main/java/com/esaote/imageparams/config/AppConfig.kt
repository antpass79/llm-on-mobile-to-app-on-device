package com.esaote.imageparams.config

import com.esaote.imageparams.BuildConfig

object AppConfig {
    val wsUrl: String = BuildConfig.WS_URL
    val llmUrl: String = BuildConfig.LLM_URL
    val llmKey: String = BuildConfig.LLM_KEY
    val llmModel: String = BuildConfig.LLM_MODEL
}

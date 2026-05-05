package com.esaote.imageparams.config

import com.esaote.imageparams.BuildConfig

object AppConfig {
    val wsUrl: String = BuildConfig.WS_URL
    val geminiKey: String = BuildConfig.GEMINI_KEY
    val azureUrl: String = BuildConfig.AZURE_URL
    val azureKey: String = BuildConfig.AZURE_KEY
    val azureModel: String = BuildConfig.AZURE_MODEL
}

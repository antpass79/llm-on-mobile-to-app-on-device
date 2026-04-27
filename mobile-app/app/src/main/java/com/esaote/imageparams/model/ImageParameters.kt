package com.esaote.imageparams.model

import kotlinx.serialization.Serializable

@Serializable
data class ImageParameters(
    val gain: Int = 50,
    val depth: Int = 100,
    val zoom: Int = 1,
)

package com.esaote.imageparams.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MessageType {
    @SerialName("update")
    UPDATE,

    @SerialName("sync")
    SYNC,
}

@Serializable
data class ParameterPayload(
    val gain: Int? = null,
    val depth: Int? = null,
    val zoom: Int? = null,
)

@Serializable
data class ParameterMessage(
    val type: MessageType,
    val payload: ParameterPayload,
)

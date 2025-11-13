package io.github.lowley.common

import kotlinx.serialization.Serializable

@Serializable
data class ServerMessage(
    val text: MessageText
){



}

@Serializable
@JvmInline
value class MessageText(val value: String)

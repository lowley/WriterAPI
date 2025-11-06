package io.github.lowley.common

import kotlinx.serialization.Serializable

@Serializable
data class RichLogEvent(
    val timestampMillis: Long,
    val tag: TagType,
    val level: LevelType,
    val message: MessageType
)

@Serializable
@JvmInline
value class TagType(val value: String)

@Serializable
@JvmInline
value class LevelType(val value: String)

@Serializable
@JvmInline
value class MessageType(val value: String)
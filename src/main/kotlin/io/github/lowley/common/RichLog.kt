package io.github.lowley.common

import kotlinx.serialization.Serializable
import org.jsoup.nodes.Element
import java.util.Calendar

@Serializable
data class RichLog(
    val timestampMillis: Long = Calendar.getInstance().timeInMillis,
    val richText: RichText

)

@Serializable
data class RichText(
    val richSegments: List<RichSegment>
) {

}

@Serializable
data class RichSegment(
    val text: TextType,
    val style: Style,
)

@Serializable
@JvmInline
value class TextType(val text: String){

    override fun toString(): String {
        return text
    }
}

@Serializable
data class Style(
    val bold: Boolean = false,
) {

}


@Serializable
@JvmInline
value class TagType(val value: String)

@Serializable
@JvmInline
value class LevelType(val value: String)

@Serializable
@JvmInline
value class MessageType(val value: String)
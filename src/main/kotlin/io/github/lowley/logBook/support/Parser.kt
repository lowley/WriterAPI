package io.github.lowley.logBook.support

import io.github.lowley.common.RichSegment
import io.github.lowley.common.Style
import io.github.lowley.common.TextType
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

class Parser: IParser {

    override fun parse(raw: LogMessage): List<RichSegment> {

        val doc = Jsoup.parseBodyFragment(raw.text)
        val newSegments = mutableListOf<RichSegment>()

        fun walk(node: Node, style: Style){
            when(node){
                is TextNode -> {
                    val text = node.text()
                    if (text.isNotBlank())
                        newSegments += RichSegment(
                            text = TextType(text),
                            style = style
                        )
                }

                is Element -> {
                    val newStyle = style.enrichWith(node)
                    node.childNodes().forEach { child ->
                        walk(child, newStyle)
                    }
                }
            }
        }

        walk(doc.body(), Style())
        return newSegments
    }

    fun Style.enrichWith(element: Element): Style {
        var style = this
        when (element.tagName().lowercase()) {
            "b", "strong", "bold" -> style = style.copy(bold = true)
//            "i", "em"     -> s = s.copy(italic = true)
            "u"           -> style = style.copy(underline = true)
//            "span"        -> {
//                val styleAttr = element.attr("style")
//                // ici tu peux parser styleAttr pour trouver "color: #xxxxxx"
//                // et mettre s = s.copy(color = "#xxxxxx")
//            }
        }
        return style
    }
}


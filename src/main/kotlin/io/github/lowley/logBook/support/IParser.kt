package io.github.lowley.logBook.support

import io.github.lowley.common.RichSegment

interface IParser {

    fun parse(raw: LogMessage): List<RichSegment>


}
package io.github.lowley.logBook

import io.github.lowley.common.RichSegment

interface IParser {

    fun parse(raw: LogMessage): List<RichSegment>


}
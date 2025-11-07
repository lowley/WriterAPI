package io.github.lowley.emitter

import io.github.lowley.common.RichSegment

interface IParser {

    fun parse(raw: LogMessage): List<RichSegment>


}
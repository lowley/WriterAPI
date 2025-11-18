package io.github.lowley.logBook

import io.github.lowley.common.RichLog
import io.github.lowley.common.RichSegment
import io.github.lowley.common.RichText
import io.github.lowley.engineRoom.submarine.DiveLogging
import io.github.lowley.engineRoom.submarine.utils.DiveStateMachineManager
import io.github.lowley.logBook.support.IParser
import io.github.lowley.logBook.support.LogMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

////////////////////
// point d'entrée //
////////////////////

fun write(block: LogBuilder.() -> Unit) {

    val builder: LogBuilder = DiveStateMachineManager.InitializeAppLogging.koin.get()

    builder.block()
    builder.postTreatment()
}

////////////////////////////////////////
// classe contenant les appels métier //
////////////////////////////////////////

class LogBuilder(
    val parser: IParser,
) {
    val segments = mutableListOf<RichSegment>()
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * traitement final de la ligne de log
     */
    fun postTreatment() {
        val richLog = RichLog(
            richText = RichText(segments.toList())
        )

        send(richLog)
    }

    private fun send(richLog: RichLog) {
        scope.launch {
            DiveLogging.sendLogToAPI(richLog)
        }

        segments.clear()
    }
}

///////////////////
// appels métier //
///////////////////

fun LogBuilder.log(msg: String) {
    val newSegments: List<RichSegment> = parser.parse(LogMessage(msg))
    segments.addAll(newSegments)
}


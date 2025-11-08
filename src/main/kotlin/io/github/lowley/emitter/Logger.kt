package io.github.lowley.emitter

import io.github.lowley.common.RichLog
import io.github.lowley.common.RichSegment
import io.github.lowley.common.RichText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

////////////////////
// point d'entrée //
////////////////////

fun write(block: LogBuilder.() -> Unit) {
    val koin = GlobalContext.get()
    val builder: LogBuilder = koin.get()

    builder.block()
    builder.postTreatment()
}

////////////////////////////////////////
// classe contenant les appels métier //
////////////////////////////////////////

class LogBuilder(
    val parser: IParser,
    val api: ILoggerCommunicationAPI
) {
    val segments = mutableListOf<RichSegment>()
    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * traitement final de la ligne de log
     */
    fun postTreatment() {
        val richLog = RichLog(
            richText = RichText(segments)
        )

        send(richLog)
    }

    private fun send(richLog: RichLog) {
        scope.launch {
            val result = api.sendRichLog(richLog, 7777)
            result.fold(
                ifLeft = {
                    println("erreur d'envoi d'un richLog ${richLog.richText}")
                },
                ifRight = {

                }
            )
        }

        segments.clear()
    }

    fun test() {
        write {
            log("truc")
            log("machin")
        }
    }
}

///////////////////
// appels métier //
///////////////////

fun LogBuilder.log(msg: String) {
    val newSegments: List<RichSegment> = parser.parse(LogMessage(msg))
    segments.addAll(newSegments)
}


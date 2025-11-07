package io.github.lowley.emitter

import io.github.lowley.common.RichSegment
import org.koin.core.context.GlobalContext

////////////////////
// point d'entrée //
////////////////////

fun write(block: LogBuilder.() -> Unit){
    val koin = GlobalContext.get()
    val builder: LogBuilder = koin.get()

    builder.block()
    builder.buildAndSend()
}

////////////////////////////////////////
// classe contenant les appels métier //
////////////////////////////////////////

class LogBuilder(
    val parser: IParser
){
    val segments = mutableListOf<RichSegment>()

    fun buildAndSend(){



    }

    fun test(){
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


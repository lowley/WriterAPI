package io.github.lowley.version2.common

import io.github.lowley.common.AdbError

//////////////////
// StateMessage //
//////////////////

@JvmInline
value class StateMessage(val message: String){
    companion object {
        val EMPTY: StateMessage = StateMessage("EMPTY")
    }
}


internal fun String.toStateMessage(): StateMessage = StateMessage(this)

//////////////////
// ErrorMessage //
//////////////////

@JvmInline
value class ErrorMessage(val text: String) {

    fun toStateMessage(): StateMessage = StateMessage(text)
}

internal fun AdbError.toErrorMessage(): ErrorMessage = ErrorMessage(
    when (this) {
        is AdbError.ExceptionThrown -> this.throwable.message
        is AdbError.CommandFailed -> this.output

    } ?: "erreur inconnue"
)

internal fun String.toErrorMessage(): ErrorMessage = ErrorMessage(this)

////////////
// divers //
////////////

object Success

enum class NetworkBehavior(text: String){
    Emitter("Emitter"),
    Receiver("Receiver"),
}

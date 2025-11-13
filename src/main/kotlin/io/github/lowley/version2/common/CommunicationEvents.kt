package io.github.lowley.version2.common

import io.github.lowley.common.AdbError
import ru.nsk.kstatemachine.event.Event
import java.net.ServerSocket
import java.net.Socket

interface WEvent: Event

internal sealed class AppEvent(){
    data class StartListening(val serverSocket: ServerSocket): Event, AppEvent()
    object Disconnect : Event, AppEvent()
    data class Connect(val socket: Socket) : Event, AppEvent()
    data class GoOnError(val text: ErrorMessage) : Event, AppEvent()
    object Disable: Event, AppEvent()
}

@JvmInline
value class ErrorMessage(val text: String){

    fun toStateMessage(): StateMessage = StateMessage(text)
}

internal fun AdbError.toErrorMessage(): ErrorMessage = ErrorMessage(
    when (this){
        is AdbError.ExceptionThrown -> this.throwable.message
        is AdbError.CommandFailed -> this.output

    } ?: "erreur inconnue"
)

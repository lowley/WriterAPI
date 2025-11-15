package io.github.lowley.version2.common

import io.github.lowley.common.AdbError
import ru.nsk.kstatemachine.event.Event

internal sealed class Events {
    object Listen : Events(), Event
    object Disconnect : Events(), Event
    object Connect : Events(), Event
    data class GoOnError(val text: ErrorMessage) : Events(), Event
    object Disable : Events(), Event
}

//submarine
internal typealias DiveListen     = Events.Listen
internal typealias DiveConnect    = Events.Connect
internal typealias DiveDisconnect = Events.Disconnect
internal typealias DiveGoOnError  = Events.GoOnError

//boat
internal typealias SurfaceConnect    = Events.Connect
internal typealias SurfaceDisconnect = Events.Disconnect
internal typealias SurfaceGoOnError  = Events.GoOnError
internal typealias SurfaceDisable  = Events.Disable

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
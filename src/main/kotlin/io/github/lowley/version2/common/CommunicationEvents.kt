package io.github.lowley.version2.common

import io.github.lowley.common.AdbError
import ru.nsk.kstatemachine.event.Event

internal sealed interface DiveEvent         //app android
internal sealed interface SurfaceEvent      //boat

internal object Listen: Event, SurfaceEvent
internal object Disconnect : Event, DiveEvent, SurfaceEvent
internal object Connect : Event, DiveEvent, SurfaceEvent
internal data class GoOnError(val text: ErrorMessage) : Event, DiveEvent, SurfaceEvent
internal object Disable: Event, SurfaceEvent


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

internal fun String.toErrorMessage(): ErrorMessage = ErrorMessage(this)
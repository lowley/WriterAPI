package io.github.lowley.version2.viewer

import io.github.lowley.common.RichLog
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IViewerAppComponent {
    object Success

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    val stateMessageFlow: StateFlow<StateMessage>
    fun emitStateMessage(stateMessage: StateMessage)

    ////////////////////////////////
    // interrupteur de la machine //
    ////////////////////////////////
    val isLoggingEnabledFlow: StateFlow<Boolean>
    fun enableLogs(enabled: Boolean)
    fun disableLogs()
    fun enableLogs()

    /////////////////////////
    // flux des logs reçus //
    /////////////////////////
    val logs: SharedFlow<RichLog>
}

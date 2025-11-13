package io.github.lowley.version2.viewer

import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IViewerAppComponent {
    object Success

    //////////////////////////
    // démarrage du service //
    //////////////////////////
    fun ensureMachineStarted()

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    val stateMessageFlow: StateFlow<StateMessage>
    fun sendStateMessageToViewer(stateMessage: StateMessage)

    //////////////////////////////////
    // activation des logs de l'app //
    //////////////////////////////////
    val isLoggingEnabledFlow: StateFlow<Boolean>
    fun enableLogs(enabled: Boolean)
    fun disableLogs()
    fun enableLogs()

    /////////////////////////
    // flux des logs reçus //
    /////////////////////////
    val logFlow: SharedFlow<RichLog>

    //////////////////////////////////////////////
    // envoi d'un ServerMessage à l'app Android //
    //////////////////////////////////////////////
    fun emit(message: ServerMessage)
}


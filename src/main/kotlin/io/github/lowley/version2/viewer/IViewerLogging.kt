package io.github.lowley.version2.viewer

import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.app.AppLogging
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IViewerLogging {

    //////////////////////////
    // démarrage du service //
    //////////////////////////
    fun startService()

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    val stateMessageFlow: StateFlow<StateMessage>

    //////////////////////////////////
    // activation des logs de l'app //
    //////////////////////////////////
    val isLoggingEnabledFlow: StateFlow<Boolean>
    fun toggleLogs(enabled: Boolean)
    fun enableLogs()
    fun disableLogs()

    /////////////////////////
    // flux des logs reçus //
    /////////////////////////
    val logFlow: SharedFlow<RichLog>

    //////////////////////////////////////////////
    // envoi d'un ServerMessage à l'app Android //
    //////////////////////////////////////////////

    fun sendMessageToApp(message: ServerMessage)
}


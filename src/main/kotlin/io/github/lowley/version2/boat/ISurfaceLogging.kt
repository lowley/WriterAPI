package io.github.lowley.version2.boat

import arrow.core.Either
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.common.StateMessage
import io.github.lowley.version2.common.Success
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface ISurfaceLogging {

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

    suspend fun sendMessageToApp(message: ServerMessage): Either<AdbError, Success>
}


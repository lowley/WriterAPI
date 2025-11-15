package io.github.lowley.version2.app

import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IAppLogging {

    //////////////////////////
    // démarrage du service //
    //////////////////////////
    fun startService()

    ////////////////////////
    // envoi d'un message //
    ////////////////////////
    fun sendLogToAPI(log: RichLog)

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    val stateMessageFlow: StateFlow<StateMessage>
}


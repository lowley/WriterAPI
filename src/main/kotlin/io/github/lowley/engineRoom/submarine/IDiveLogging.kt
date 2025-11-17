package io.github.lowley.engineRoom.submarine

import io.github.lowley.common.RichLog
import io.github.lowley.engineRoom.common.StateMessage
import kotlinx.coroutines.flow.StateFlow

interface IDiveLogging {

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


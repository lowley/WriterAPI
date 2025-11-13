package io.github.lowley.version2.androidApp

import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IAppLogging {
    object Success

    //////////////////////////
    // démarrage du service //
    //////////////////////////
    fun ensureMachineStarted()

    ////////////////////////
    // envoi d'un message //
    ////////////////////////
    suspend fun sendLogToServer(richLog: RichLog)

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    val stateMessage: StateFlow<StateMessage>
    fun setStateMessage(stateMessage: StateMessage)

    /////////////////////////////////////////////
    // flux des ServerMessage reçus du serveur //
    /////////////////////////////////////////////
    val messages: SharedFlow<ServerMessage>

}


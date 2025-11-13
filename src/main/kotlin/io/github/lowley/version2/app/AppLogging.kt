package io.github.lowley.version2.app

import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.app.utils.InitializeAppLogging
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AppLogging : IAppLogging {

    //////////////////////////
    // démarrage du service //
    //////////////////////////
    override fun startService() {
        InitializeAppLogging
    }

    ////////////////////////
    // envoi d'un messgae //
    ////////////////////////
    override suspend fun sendLogToServer(richLog: RichLog) {


    }


    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    private val _stateMessage = MutableStateFlow(StateMessage.EMPTY)
    override val stateMessage = _stateMessage.asStateFlow()

    override fun setStateMessage(stateMessage: StateMessage) {
        _stateMessage.update { stateMessage }
    }

    /////////////////////////////////////////////
    // flux des ServerMessage reçus du serveur //
    /////////////////////////////////////////////
    private val _messages = MutableSharedFlow<ServerMessage>(replay = 0, extraBufferCapacity = 64)

    //#[[plogs]]
    override val messages = _messages.asSharedFlow()

    /**
     * Méthode interne pour envoyer les ServerMessages reçus vers "message" qui est lu par l'app
     * @see AppLogging.logs
      */
    // voir [[plogs]]
    suspend fun resendToApp(log: ServerMessage) {
        _messages.emit(log)
    }


}


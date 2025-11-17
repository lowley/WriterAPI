package io.github.lowley.version2.submarine

import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import io.github.lowley.version2.submarine.utils.DiveStateMachineManager.InitializeAppLogging

object DiveLogging : IDiveLogging {

    //////////////////////////
    // démarrage du service //
    //////////////////////////
    override fun startService() {
        InitializeAppLogging
    }

    ////////////////////////
    // envoi d'un messgae //
    ////////////////////////
    /**
     * c'est envoyé par la StateMachine, quand elle se trouvera en état Connected
     */
    internal val logsToBeSentToViewer = Channel<RichLog>(capacity = Channel.BUFFERED)

    // voir [[RichLog.addToLogsToBeSentToViewer]]
    override fun sendLogToAPI(log: RichLog) {
        logsToBeSentToViewer.trySend(log)
    }

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    private val _stateMessageFlow = MutableStateFlow(StateMessage.EMPTY)
    override val stateMessageFlow = _stateMessageFlow.asStateFlow()

    fun setStateMessage(stateMessage: StateMessage) {
        _stateMessageFlow.update { stateMessage }
    }

    /////////////////////////////////////////////
    // flux des ServerMessage reçus du serveur //
    /////////////////////////////////////////////
    private val _messages = MutableSharedFlow<ServerMessage>(replay = 0, extraBufferCapacity = 64)

    //#[[plogs]]
    val messages = _messages.asSharedFlow()

    /**
     * Méthode interne pour envoyer les ServerMessages reçus vers "message" qui est lu par l'app
     * @see DiveLogging.logs
      */
    // voir [[plogs]]
    internal suspend fun sendMessageToApp(log: ServerMessage) {
        _messages.emit(log)
    }
}

// #[[RichLog.addToLogsToBeSentToViewer]]
fun RichLog.addToLogsToBeSentToViewer() {
    DiveLogging.sendLogToAPI(this)
}


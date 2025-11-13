package io.github.lowley.version2.viewer

import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.common.StateMessage
import io.github.lowley.version2.viewer.utils.InitializeViewerLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ViewerLogging : IViewerLogging {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    //////////////////////////
    // Démarrage du service //
    //////////////////////////
    override fun startService() {
        InitializeViewerLogging
    }

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    private val _stateMessageFlow = MutableStateFlow(StateMessage.EMPTY)

    override val stateMessageFlow = _stateMessageFlow.asStateFlow()

    override fun sendStateMessageToViewer(stateMessage: StateMessage) {
        _stateMessageFlow.update { stateMessage }
    }

    //////////////////////////////////
    // activation des logs de l'app //
    //////////////////////////////////
    private val _isLoggingEnabledFlow = MutableStateFlow<Boolean>(false)
    override val isLoggingEnabledFlow = _isLoggingEnabledFlow.asStateFlow()

    override fun enableLogs(enabled: Boolean) {
        _isLoggingEnabledFlow.update { enabled }
    }

    override fun enableLogs() {
        _isLoggingEnabledFlow.update { true }
    }

    override fun disableLogs() {
        _isLoggingEnabledFlow.update { false }
    }

    /////////////////////////
    // flow des logs reçus //
    /////////////////////////
    private val _logs = MutableSharedFlow<RichLog>(
        replay = 0,
        extraBufferCapacity = 64
    )

    override val logFlow = _logs.asSharedFlow()

    //////////////////////////////////////////////
    // envoi d'un ServerMessage à l'app Android //
    //////////////////////////////////////////////
    override fun emit(message: ServerMessage) {
        //TODO compléter
    }
}
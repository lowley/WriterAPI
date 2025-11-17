package io.github.lowley.version2.boat

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.version2.common.StateMessage
import io.github.lowley.version2.common.Success
import io.github.lowley.version2.boat.utils.InitializeViewerLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object SurfaceLogging : ISurfaceLogging {

    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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

    internal fun setStateMessage(stateMessage: StateMessage) {
        _stateMessageFlow.update { stateMessage }
    }

    //////////////////////////////////
    // activation des logs de l'app //
    //////////////////////////////////
    private val _isLoggingEnabledFlow = MutableStateFlow<Boolean>(false)
    override val isLoggingEnabledFlow = _isLoggingEnabledFlow.asStateFlow()

    override fun toggleLogs(enabled: Boolean) {
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

    internal fun sendLogToViewer(log: RichLog) {
        scope.launch(Dispatchers.IO) {
            _logs.emit(log)
        }
    }

    //////////////////////////////////////
    // envoi d'un ServerMessage à l'app //
    //////////////////////////////////////
    internal val messagesToBeSentToApp = Channel<ServerMessage>(capacity = Channel.BUFFERED)

    // voir [[addToLogsToBeSentToViewer]]
    override suspend fun sendMessageToApp(message: ServerMessage): Either<AdbError, Success> = either {

        try {
            messagesToBeSentToApp.send(message)
            return Success.right()

        } catch (ex: Exception) {
            raise(AdbError.ExceptionThrown(ex))
        }
    }
}

// #[[addToLogsToBeSentToViewer]]
fun ServerMessage.addToLogsToBeSentToViewer() {
    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    scope.launch {
        SurfaceLogging.sendMessageToApp(this@addToLogsToBeSentToViewer)
    }
}



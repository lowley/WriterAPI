package io.github.lowley.version2.viewer

import io.github.lowley.common.RichLog
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object ViewerAppComponent: IViewerAppComponent {

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    //////////////////////////////////////
    // message d'état affiché dans l'UI //
    //////////////////////////////////////
    private val _stateMessage = MutableStateFlow(StateMessage.EMPTY)
    override val stateMessageFlow = _stateMessage.asStateFlow()

    override fun emitStateMessage(stateMessage: StateMessage) {
        _stateMessage.update { stateMessage }
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

    override fun emit(log: RichLog){
        scope.launch(Dispatchers.Main) {
            _logs.emit(log)
        }
    }

    // 2) Vue en lecture seule pour l’extérieur
    override val logs = _logs.asSharedFlow()


}
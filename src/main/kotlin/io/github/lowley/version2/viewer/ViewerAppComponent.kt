package io.github.lowley.version2.viewer

import io.github.lowley.common.RichLog
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ViewerAppComponent: IViewerAppComponent {

    //////////////////////////////////////
    // message d'état affiché dans l'UI //
    //////////////////////////////////////
    private val _stateMessage = MutableStateFlow<StateMessage>(StateMessage.EMPTY)
    override val stateMessage = _stateMessage.asStateFlow()

    override fun setStateMessage(stateMessage: StateMessage) {
        _stateMessage.update { stateMessage }
    }

    //////////////////////////////////
    // activation des logs de l'app //
    //////////////////////////////////
    private val _androidAppLogEnabled = MutableStateFlow<Boolean>(false)
    override val androidAppLogEnabled = _androidAppLogEnabled.asStateFlow()

    override fun setAndroidAppLogEnabled(enabled: Boolean) {
        _androidAppLogEnabled.update { enabled }
    }

    /////////////////////////
    // flow des logs reçus //
    /////////////////////////
    private val _logs = MutableSharedFlow<RichLog>(
        replay = 0,
        extraBufferCapacity = 64
    )

    // 2) Vue en lecture seule pour l’extérieur
    override val logs = _logs.asSharedFlow()


}
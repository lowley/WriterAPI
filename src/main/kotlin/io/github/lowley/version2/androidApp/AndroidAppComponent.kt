package io.github.lowley.version2.androidApp

import arrow.core.right
import io.github.lowley.common.RichLog
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class AndroidAppComponent: IAndroidAppComponent {

    //////////////////////////////////////
    // message d'état affiché dans l'UI //
    //////////////////////////////////////
    private val _stateMessage = MutableStateFlow<StateMessage>(StateMessage.EMPTY)
    override val stateMessage = _stateMessage.asStateFlow()

    override fun setStateMessage(stateMessage: StateMessage) {
        _stateMessage.update { stateMessage }
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

    override suspend fun emit(log: RichLog) {
        _logs.emit(log)
    }

    ////////////////////////
    // envoi d'un messgae //
    ////////////////////////
    override suspend fun sendRichLog(richLog: RichLog, port: Int) =
        withContext(Dispatchers.IO) {

        ensureMachineStarted()
        return@withContext IAndroidAppComponent.Success.right()


    }

    private fun ensureMachineStarted() {

    }


}
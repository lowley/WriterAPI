package io.github.lowley.version2.viewer

import io.github.lowley.common.RichLog
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IViewerAppComponent {
    object Success

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    val stateMessage: StateFlow<StateMessage>
    fun setStateMessage(stateMessage: StateMessage)

    ////////////////////////////////
    // interrupteur de la machine //
    ////////////////////////////////
    val androidAppLogEnabled: StateFlow<Boolean>
    fun setAndroidAppLogEnabled(enabled: Boolean)

    /////////////////////////
    // flux des logs reçus //
    /////////////////////////
    val logs: SharedFlow<RichLog>
    fun emit(log: RichLog)
}


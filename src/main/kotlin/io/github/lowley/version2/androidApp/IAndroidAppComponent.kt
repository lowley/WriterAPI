package io.github.lowley.version2.androidApp

import arrow.core.Either
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.version2.common.StateMessage
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface IAndroidAppComponent {
    object Success

    ////////////////////////
    // envoi d'un message //
    ////////////////////////
    suspend fun sendRichLog(
        richLog: RichLog,
        port: Int = 7777
    ): Either<AdbError, Success>

    /////////////////////////////////////
    // message d'info de l'état actuel //
    /////////////////////////////////////
    val stateMessage: StateFlow<StateMessage>
    fun setStateMessage(stateMessage: StateMessage)


    ////////////////////////
    // flux des logs émis //
    ////////////////////////
    val logs: SharedFlow<RichLog>
    suspend fun emit(log: RichLog)




}


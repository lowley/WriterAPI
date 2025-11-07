package io.github.lowley.emitter

import arrow.core.Either
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog

interface ILoggerCommunicationAPI {

    object Success

    suspend fun sendRichLog(
        richLog: RichLog,
        port: Int = 7777
    ): Either<AdbError, Success>

}

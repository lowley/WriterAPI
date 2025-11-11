package io.github.lowley.receiver

import arrow.core.Either
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import kotlinx.coroutines.flow.Flow
import java.net.Socket

interface IDeviceAPI {

    fun deviceLogEvents(port: Int = 7777): Either<AdbError, Flow<RichLog>>
    fun reverseAdbPort(port: Int = 7777): Either<AdbError, Unit>
    fun readClientLines(client: Socket, onLineReceived: suspend (line: String) -> Unit)
    fun close()

}
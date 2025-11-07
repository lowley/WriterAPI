package io.github.lowley.receiver

import arrow.core.Either
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import kotlinx.coroutines.flow.Flow

interface IDeviceAPI {

    fun deviceLogEvents(port: Int = 7777): Either<AdbError, Flow<RichLog>>



}
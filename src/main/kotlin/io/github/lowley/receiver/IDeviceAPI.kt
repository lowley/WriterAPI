package io.github.lowley.receiver

import arrow.core.Either
import io.github.lowley.common.RichLogEvent
import io.github.lowley.receiver.DeviceAPI.AdbError
import kotlinx.coroutines.flow.Flow

interface IDeviceAPI {

    fun deviceLogEvents(port: Int = 7777): Either<AdbError, Flow<RichLogEvent>>



}
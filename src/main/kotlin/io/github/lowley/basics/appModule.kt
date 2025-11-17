package lorry.basics

import io.github.lowley.emitter.ILoggerCommunicationAPI
import io.github.lowley.emitter.IParser
import io.github.lowley.emitter.LogBuilder
import io.github.lowley.emitter.LoggerCommunicationAPI
import io.github.lowley.emitter.Parser
import io.github.lowley.receiver.DeviceAPI
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.submarine.DiveLogging
import io.github.lowley.version2.submarine.IDiveLogging
import io.github.lowley.version2.boat.ISurfaceLogging
import io.github.lowley.version2.boat.SurfaceLogging

import org.koin.dsl.module
import io.github.lowley.version2.submarine.utils.DiveStateMachineManager
import io.github.lowley.version2.boat.utils.SurfaceStateMachineManager

val appModule = module {
    single<IParser> { Parser() }
    single<ILoggerCommunicationAPI> { LoggerCommunicationAPI() }
    single { LogBuilder(get(), get()) }
    single<IDeviceAPI> { DeviceAPI() }

    single<IDiveLogging> { DiveLogging }
    single<DiveLogging> { DiveLogging }

    single<ISurfaceLogging> { SurfaceLogging }
    single<SurfaceLogging> { SurfaceLogging }

    single {  DiveStateMachineManager(get(), get()) }
    single {  SurfaceStateMachineManager(get(), get()) }
}
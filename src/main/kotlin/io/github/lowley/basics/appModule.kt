package lorry.basics

import io.github.lowley.logBook.IParser
import io.github.lowley.logBook.LogBuilder
import io.github.lowley.logBook.Parser
import io.github.lowley.receiver.DeviceAPI
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.engineRoom.submarine.DiveLogging
import io.github.lowley.engineRoom.submarine.IDiveLogging
import io.github.lowley.engineRoom.boat.ISurfaceLogging
import io.github.lowley.engineRoom.boat.SurfaceLogging

import org.koin.dsl.module
import io.github.lowley.engineRoom.submarine.utils.DiveStateMachineManager
import io.github.lowley.engineRoom.boat.utils.SurfaceStateMachineManager

val appModule = module {
    single<IParser> { Parser() }
    single { LogBuilder(get()) }
    single<IDeviceAPI> { DeviceAPI() }

    single<IDiveLogging> { DiveLogging }
    single<DiveLogging> { DiveLogging }

    single<ISurfaceLogging> { SurfaceLogging }
    single<SurfaceLogging> { SurfaceLogging }

    single {  DiveStateMachineManager() }
    single {  SurfaceStateMachineManager() }
}
package lorry.basics

import io.github.lowley.logBook.support.IParser
import io.github.lowley.logBook.LogBuilder
import io.github.lowley.logBook.support.Parser
import io.github.lowley.engineRoom.submarine.DiveLogging
import io.github.lowley.engineRoom.submarine.IDiveLogging
import io.github.lowley.engineRoom.boat.ISurfaceLogging
import io.github.lowley.engineRoom.boat.SurfaceLogging

import org.koin.dsl.module
import io.github.lowley.engineRoom.submarine.utils.DiveStateMachineManager
import io.github.lowley.engineRoom.boat.utils.SurfaceStateMachineManager

val periscopeInjections = module {
    single<IParser> { Parser() }
    single { LogBuilder(get()) }

    single<IDiveLogging> { DiveLogging }
    single<DiveLogging> { DiveLogging }

    single<ISurfaceLogging> { SurfaceLogging }
    single<SurfaceLogging> { SurfaceLogging }

    single {  DiveStateMachineManager() }
    single {  SurfaceStateMachineManager() }
}
package lorry.basics

import io.github.lowley.emitter.ILoggerCommunicationAPI
import io.github.lowley.emitter.IParser
import io.github.lowley.emitter.LogBuilder
import io.github.lowley.emitter.LoggerCommunicationAPI
import io.github.lowley.emitter.Parser
import io.github.lowley.receiver.DeviceAPI
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.dive.AppLogging
import io.github.lowley.version2.dive.IAppLogging
import io.github.lowley.version2.surface.IViewerLogging
import io.github.lowley.version2.surface.ViewerLogging

import org.koin.dsl.module
import io.github.lowley.version2.dive.utils.AppStateMachineManager
import io.github.lowley.version2.surface.utils.ViewerStateMachineManager

val appModule = module {
    single<IParser> { Parser() }
    single<ILoggerCommunicationAPI> { LoggerCommunicationAPI() }
    single { LogBuilder(get(), get()) }
    single<IDeviceAPI> { DeviceAPI() }

    single<IAppLogging> { AppLogging }
    single<AppLogging> { AppLogging }

    single<IViewerLogging> { ViewerLogging }
    single<ViewerLogging> { ViewerLogging }

    single {  AppStateMachineManager(get(), get()) }
    single {  ViewerStateMachineManager(get(), get()) }
}
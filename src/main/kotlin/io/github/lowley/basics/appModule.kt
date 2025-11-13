package lorry.basics

import io.github.lowley.emitter.ILoggerCommunicationAPI
import io.github.lowley.emitter.IParser
import io.github.lowley.emitter.LogBuilder
import io.github.lowley.emitter.LoggerCommunicationAPI
import io.github.lowley.emitter.Parser
import io.github.lowley.receiver.DeviceAPI
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.androidApp.AppLogging
import io.github.lowley.version2.androidApp.IAppLogging
import io.github.lowley.version2.viewer.IViewerLogging
import io.github.lowley.version2.viewer.ViewerLogging

import org.koin.dsl.module

val appModule = module {
    single<IParser> { Parser() }
    single<ILoggerCommunicationAPI> { LoggerCommunicationAPI() }
    single { LogBuilder(get(), get()) }
    single<IDeviceAPI> { DeviceAPI() }
    single<IAppLogging> { AppLogging() }
    single<IViewerLogging> { ViewerLogging }
    single<ViewerLogging> { ViewerLogging }
    single {  io.github.lowley.version2.androidApp.utils.AppStateMachineManager(get(), get()) }
    single {  io.github.lowley.version2.viewer.utils.ViewerStateMachineManager(get(), get()) }
}
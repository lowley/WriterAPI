package lorry.basics

import io.github.lowley.emitter.ILoggerCommunicationAPI
import io.github.lowley.emitter.IParser
import io.github.lowley.emitter.LogBuilder
import io.github.lowley.emitter.LoggerCommunicationAPI
import io.github.lowley.emitter.Parser
import io.github.lowley.receiver.DeviceAPI
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.app.AppLogging
import io.github.lowley.version2.app.IAppLogging
import io.github.lowley.version2.viewer.IViewerLogging
import io.github.lowley.version2.viewer.ViewerLogging

import org.koin.dsl.module
import io.github.lowley.version2.app.utils.AppStateMachineManager
import io.github.lowley.version2.viewer.utils.ViewerStateMachineManager

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
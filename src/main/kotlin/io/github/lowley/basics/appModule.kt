package lorry.basics

import io.github.lowley.emitter.ILoggerCommunicationAPI
import io.github.lowley.emitter.IParser
import io.github.lowley.emitter.LogBuilder
import io.github.lowley.emitter.LoggerCommunicationAPI
import io.github.lowley.emitter.Parser
import io.github.lowley.receiver.DeviceAPI
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.androidApp.AppComponent
import io.github.lowley.version2.androidApp.IAppComponent
import io.github.lowley.version2.viewer.IViewerAppComponent
import io.github.lowley.version2.viewer.ViewerAppComponent

import org.koin.dsl.module

val appModule = module {
    single<IParser> { Parser() }
    single<ILoggerCommunicationAPI> { LoggerCommunicationAPI() }
    single { LogBuilder(get(), get()) }
    single<IDeviceAPI> { DeviceAPI() }
    single<IAppComponent> { AppComponent() }
    single<IViewerAppComponent> { ViewerAppComponent }
    single<ViewerAppComponent> { ViewerAppComponent }
    single {  io.github.lowley.version2.androidApp.utils.AppStateMachineManager(get(), get()) }
    single {  io.github.lowley.version2.viewer.utils.ViewerStateMachineManager(get(), get()) }
}
package lorry.basics

import io.github.lowley.emitter.ILoggerCommunicationAPI
import io.github.lowley.emitter.IParser
import io.github.lowley.emitter.LogBuilder
import io.github.lowley.emitter.LoggerCommunicationAPI
import io.github.lowley.emitter.Parser
import io.github.lowley.receiver.DeviceAPI
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.androidApp.AndroidAppComponent
import io.github.lowley.version2.androidApp.IAndroidAppComponent
import io.github.lowley.version2.viewer.IViewerAppComponent
import io.github.lowley.version2.viewer.ViewerAppComponent

import org.koin.dsl.module

val appModule = module {
    single<IParser> { Parser() }
    single<ILoggerCommunicationAPI> { LoggerCommunicationAPI() }
    single { LogBuilder(get(), get()) }
    single<IDeviceAPI> { DeviceAPI() }
    single<IAndroidAppComponent> { AndroidAppComponent() }
    single<IViewerAppComponent> { ViewerAppComponent() }
    single {  io.github.lowley.version2.androidApp.utils.AdbComManager(get(), get()) }
    single {  io.github.lowley.version2.viewer.utils.AdbComManager(get(), get()) }
}
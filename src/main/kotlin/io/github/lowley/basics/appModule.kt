package lorry.basics

import io.github.lowley.emitter.ILoggerCommunicationAPI
import io.github.lowley.emitter.IParser
import io.github.lowley.emitter.LogBuilder
import io.github.lowley.emitter.LoggerCommunicationAPI
import io.github.lowley.emitter.Parser

import org.koin.dsl.module

val appModule = module {
    single<IParser> { Parser() }
    single<ILoggerCommunicationAPI> { LoggerCommunicationAPI() }
    single { LogBuilder(get(), get()) }

//    single<ILogCatComponent> { LogCatComponent() }
//    single { ViewerViewModel(get(), get()) }
}
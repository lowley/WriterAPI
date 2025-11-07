package lorry.basics

import io.github.lowley.emitter.IParser
import io.github.lowley.emitter.LogBuilder
import io.github.lowley.emitter.Parser

import org.koin.dsl.module

val appModule = module {
    single<IParser> { Parser() }
    single { LogBuilder(get()) }

//    single<ILogCatComponent> { LogCatComponent() }
//    single { ViewerViewModel(get(), get()) }
}
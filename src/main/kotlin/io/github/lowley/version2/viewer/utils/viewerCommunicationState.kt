package io.github.lowley.version2.viewer.utils

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.google.gson.Gson
import io.github.lowley.common.RichLog
import io.github.lowley.common.searchClient
import io.github.lowley.common.serverSocket
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.common.AppEvent
import io.github.lowley.version2.common.ErrorMessage
import io.github.lowley.version2.common.toErrorMessage
import io.github.lowley.version2.common.toStateMessage
import io.github.lowley.version2.viewer.IViewerAppComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.inject
import ru.nsk.kstatemachine.state.DefaultState
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.addState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.onExit
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.onTriggered
import kotlin.getValue
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.delay


sealed class ViewerAppStates : DefaultState() {
    object Disabled : ViewerAppStates()
    object Disconnected : ViewerAppStates()
    object Listening : ViewerAppStates()
    object Connected : ViewerAppStates()
    object Error : ViewerAppStates()
}

object AutomaticallyLaunchAdbComManager {
    init {
        val adbComManager by inject<AdbComManager>(AdbComManager::class.java)
    }
}

class AdbComManager(
    val component: IViewerAppComponent,
    val deviceAPI: IDeviceAPI
) {

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var adbComMachine: StateMachine? = null
    var serverSocket: Option<ServerSocket> = None

    init {
        scope.launch() {
            adbComMachine = getStateMachine(scope)
        }
    }

    suspend fun getStateMachine(scope: CoroutineScope): StateMachine {

        val machine = createStateMachine(scope = scope) {

            //////////////////
            // disconnected //
            //////////////////
            addInitialState(ViewerAppStates.Disconnected) {
                onEntry { scope ->
                    if (component.androidAppLogEnabled.value) {
                        val result = deviceAPI.reverseAdbPort()
                        result.fold(
                            ifLeft = { error ->
                                component.setStateMessage("reverseAdb en erreur".toStateMessage())
                                println("state Disconnected: reverseAdb en erreur")
                                machine.processEvent(AppEvent.GoOnError(error.toErrorMessage()))
                            },
                            ifRight = {
                                val result2 = serverSocket()
                                result2.fold(
                                    ifLeft = { error ->
                                        println("state Disconnected: serverSocket pas obtenu")
                                        component.setStateMessage("serverSocket pas obtenu".toStateMessage())
                                        machine.processEvent(AppEvent.GoOnError(error.toErrorMessage()))
                                    },
                                    ifRight = { seso ->
                                        println("state Disconnected: serverSocket obtenu")
                                        component.setStateMessage("serverSocket obtenu".toStateMessage())
                                        serverSocket = Some(seso)

                                        machine.processEvent(AppEvent.StartListening(seso))
                                    }
                                )
                            }
                        )
                    } else {
                        component.setStateMessage("state Disconnected: désactivation demandée".toStateMessage())
                        machine.processEvent(AppEvent.Disable)
                    }
                }
                onExit { }

                transition<AppEvent.StartListening> {
                    targetState = ViewerAppStates.Listening
                    onTriggered { scope ->
                        println("transition StartListening")
                        scope.transition.argument = scope.event.serverSocket
                    }
                }

                transition<AppEvent.GoOnError> {
                    targetState = ViewerAppStates.Error
                    onTriggered { scope ->
                        println("transition GoOnError")
                        scope.transition.argument = scope.event.text
                    }
                }

                transition<AppEvent.Disable> {
                    targetState = ViewerAppStates.Disabled
                    onTriggered { scope ->
                        println("transition Disable")
                    }
                }
            }

            ///////////////
            // listening //
            ///////////////
            addState(ViewerAppStates.Listening) {
                onEntry { scope ->

                    if (!component.androidAppLogEnabled.value) {
                        component.setStateMessage("state Listening: désactivation demandée".toStateMessage())
                        machine.processEvent(AppEvent.Disable)
                        return@onEntry
                    }

                    val serverSocket = scope.transition.argument as? ServerSocket
                    println("state Listening: socket: $serverSocket")

                    if (serverSocket != null) {
                        val result = searchClient(serverSocket)
                        result.fold(
                            ifLeft = { error ->
                                println("state Listening: erreur lors recherche client")
                                component.setStateMessage("erreur lors recherche client".toStateMessage())
                                machine.processEvent(AppEvent.GoOnError(error.toErrorMessage()))
                            },
                            ifRight = { so ->
                                println("state Listening: client obtenu")
                                component.setStateMessage("client obtenu".toStateMessage())
                                machine.processEvent(AppEvent.Connect(so))
                            }
                        )
                    }
                }
                onExit { }

                transition<AppEvent.Disconnect> {
                    targetState = ViewerAppStates.Disconnected
                    onTriggered { scope ->
                        println("transition Disconnect")
                    }
                }

                transition<AppEvent.Connect> {
                    targetState = ViewerAppStates.Connected
                    onTriggered { scope ->
                        println("transition Connect")
                        scope.transition.argument = scope.event.socket
                    }
                }

                transition<AppEvent.GoOnError> {
                    targetState = ViewerAppStates.Error
                    onTriggered { scope ->
                        println("transition GoOnError")
                        scope.transition.argument = scope.event.text
                    }
                }

                transition<AppEvent.Disable> {
                    targetState = ViewerAppStates.Disabled
                    onTriggered { scope ->
                        println("transition Disable")
                    }
                }
            }

            ///////////////
            // connected //
            ///////////////
            addState(ViewerAppStates.Connected) {
                onEntry { scope ->
                    if (!component.androidAppLogEnabled.value) {
                        component.setStateMessage("state Connected: désactivation demandée".toStateMessage())
                        machine.processEvent(AppEvent.Disable)
                        return@onEntry
                    }

                    val socket = scope.transition.argument as? Socket
                    if (socket != null) {
                        deviceAPI.readClientLines(socket) { line ->
                            try {
                                val event = Gson().fromJson(line, RichLog::class.java)
                                println("state Connecté: reçu log (brut=${event.raw()})")
                                //component.emit(event)
                            } catch (ex: Exception) {
                                println("state Connecté: erreur lors parsing de : $line")
                                component.setStateMessage("erreur lors parsing".toStateMessage())
                            }
                        }
                    }

                    component.setStateMessage("déconnexion initiée par le correspondant".toStateMessage())
                    machine.processEvent(AppEvent.Disconnect)
                }
                onExit { }

                transition<AppEvent.Disconnect> {
                    targetState = ViewerAppStates.Disconnected
                    onTriggered { scope ->
                        val socket = scope.transition.argument as? Socket
                        socket?.close()
                    }
                }

                transition<AppEvent.Disable> {
                    targetState = ViewerAppStates.Disabled
                    onTriggered { scope ->
                        println("transition Disable")
                    }
                }
            }

            addState(ViewerAppStates.Error) {
                onEntry { scope ->
                    if (!component.androidAppLogEnabled.value) {
                        component.setStateMessage("state Error: désactivation demandée".toStateMessage())
                        machine.processEvent(AppEvent.Disable)
                        return@onEntry
                    }

                    val message = scope.transition.argument as? ErrorMessage
                    if (message != null)
                        component.setStateMessage(message.toStateMessage())
                }
                onExit { }

                //manuel
                transition<AppEvent.Disconnect> {
                    targetState = ViewerAppStates.Disconnected
                    onTriggered { scope ->
                    }
                }

                transition<AppEvent.Disable> {
                    targetState = ViewerAppStates.Disabled
                    onTriggered { scope ->
                        println("transition Disable")
                    }
                }
            }

            addState(ViewerAppStates.Disabled) {
                onEntry { scope ->
                    val sc = CoroutineScope(Dispatchers.Default + SupervisorJob())
                    sc.launch {
                        while (true) {
                            if (component.androidAppLogEnabled.value) {
                                machine.processEvent(AppEvent.Disconnect)
                                return@launch
                            }

                            delay(500)
                        }
                    }
                }

                onExit { }

                transition<AppEvent.Disconnect> {
                    targetState = ViewerAppStates.Disconnected
                    onTriggered { scope ->
                        println("transition Disconnect")
                    }
                }
            }
        }

        return machine
    }
}

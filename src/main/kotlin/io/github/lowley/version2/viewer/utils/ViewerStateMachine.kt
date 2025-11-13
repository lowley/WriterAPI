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
import io.github.lowley.version2.viewer.ViewerLogging
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
import kotlinx.coroutines.withContext
import ru.nsk.kstatemachine.statemachine.BuildingStateMachine

internal class ViewerStateMachineManager(
    val component: ViewerLogging,
    val deviceAPI: IDeviceAPI
) {

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var adbComMachine: StateMachine? = null
    var serverSocket: Option<ServerSocket> = None

    init {
        if (adbComMachine == null) {
            scope.launch() {
                adbComMachine = getStateMachine(scope)
            }
        }
    }

    suspend fun getStateMachine(scope: CoroutineScope): StateMachine {

        val machine = createStateMachine(scope = scope) {

            with<BuildingStateMachine, Unit>(this@createStateMachine) {
                disconnectedState()
            }

            with<BuildingStateMachine, Unit>(this@createStateMachine) {
                listeningState()
            }

            with<BuildingStateMachine, Unit>(this@createStateMachine) {
                connectedState()
            }

            with<BuildingStateMachine, Unit>(this@createStateMachine) {
                errorState()
            }

            with<BuildingStateMachine, Unit>(this@createStateMachine) {
                disabledState()
            }
        }

        return machine
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////
    // disconnected //
    //////////////////
    context(scope : BuildingStateMachine)
    private suspend fun disconnectedState() = with(scope) {
        addInitialState(ViewerAppStates.Disconnected) {
            onEntry { scope ->
                if (component.isLoggingEnabledFlow.value) {
                    val result = deviceAPI.reverseAdbPort()
                    result.fold(
                        ifLeft = { error ->
                            component.sendStateMessageToViewer("ReverseAdb en erreur".toStateMessage())
                            println("state Disconnected: reverseAdb en erreur")
                            machine.processEvent(AppEvent.GoOnError(error.toErrorMessage()))
                        },
                        ifRight = {
                            val result2 = serverSocket()
                            result2.fold(
                                ifLeft = { error ->
                                    println("state Disconnected: serverSocket pas obtenu")
                                    component.sendStateMessageToViewer("ServerSocket pas obtenu".toStateMessage())
                                    machine.processEvent(AppEvent.GoOnError(error.toErrorMessage()))
                                },
                                ifRight = { seso ->
                                    println("state Disconnected: serverSocket obtenu")
                                    component.sendStateMessageToViewer("ServerSocket obtenu".toStateMessage())
                                    serverSocket = Some(seso)

                                    machine.processEvent(AppEvent.StartListening(seso))
                                }
                            )
                        }
                    )
                } else {
                    component.sendStateMessageToViewer("Désactivation demandée".toStateMessage())
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
    }

    ///////////////
    // listening //
    ///////////////
    context(scope : BuildingStateMachine)
    private suspend fun listeningState() = with(scope) {
        addState(ViewerAppStates.Listening) {
            onEntry { scope ->

                if (!component.isLoggingEnabledFlow.value) {
                    component.sendStateMessageToViewer("Désactivation demandée".toStateMessage())
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
                            component.sendStateMessageToViewer("Erreur lors recherche client".toStateMessage())
                            machine.processEvent(AppEvent.GoOnError(error.toErrorMessage()))
                        },
                        ifRight = { so ->
                            println("state Listening: client obtenu")
                            component.sendStateMessageToViewer("Client obtenu".toStateMessage())
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
    }

    ///////////////
    // connected //
    ///////////////
    context(scope : BuildingStateMachine)
    private suspend fun connectedState() = with(scope) {
        addState(ViewerAppStates.Connected) {
            onEntry { scope ->
                if (!component.isLoggingEnabledFlow.value) {
                    component.sendStateMessageToViewer("Désactivation demandée".toStateMessage())
                    machine.processEvent(AppEvent.Disable)
                    return@onEntry
                }

                val socket = scope.transition.argument as? Socket
                println("state Connected: socket=$socket")
                if (socket != null) {
                    deviceAPI.readClientLines(socket) { line ->
                        println("state Connected: line=$line")
                        try {
                            val event = Gson().fromJson(line, RichLog::class.java)
                            println("state Connecté: reçu log (brut=${event.raw()})")
                            withContext(Dispatchers.Main){
                                component.emit(event)
                                println("state Connected: log émis")
                            }

                        } catch (ex: Exception) {
                            println("state Connecté: erreur lors parsing de : $line")
                        }
                    }
                }

                component.sendStateMessageToViewer("Déconnexion initiée par le correspondant".toStateMessage())
                machine.processEvent(AppEvent.Disconnect)
            }
            onExit { }

            transition<AppEvent.Disconnect> {
                targetState = ViewerAppStates.Disconnected
                onTriggered { scope ->
                    println("transition Disconnect")
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
    }

    ///////////
    // error //
    ///////////
    context(scope : BuildingStateMachine)
    private suspend fun errorState() = with(scope) {
        addState(ViewerAppStates.Error) {
            onEntry { scope ->
                if (!component.isLoggingEnabledFlow.value) {
                    component.sendStateMessageToViewer("Désactivation demandée".toStateMessage())
                    machine.processEvent(AppEvent.Disable)
                    return@onEntry
                }

                val message = scope.transition.argument as? ErrorMessage
                if (message != null)
                    component.sendStateMessageToViewer("Erreur: $message".toStateMessage())
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
                    serverSocket.fold(
                        ifEmpty = {},
                        ifSome = { serverSocket ->
                            serverSocket.close()
                        }
                    )
                }
            }
        }
    }

    //////////////
    // disabled //
    //////////////
    context(scope : BuildingStateMachine)
    private suspend fun disabledState() = with(scope) {
        addState(ViewerAppStates.Disabled) {
            onEntry { scope ->
                val sc = CoroutineScope(Dispatchers.Default + SupervisorJob())
                sc.launch {
                    while (true) {
                        if (component.isLoggingEnabledFlow.value) {
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
}

internal sealed class ViewerAppStates : DefaultState() {
    object Disabled : ViewerAppStates()
    object Disconnected : ViewerAppStates()
    object Listening : ViewerAppStates()
    object Connected : ViewerAppStates()
    object Error : ViewerAppStates()
}

internal object InitializeViewerLogging {
    private val stateMachine: ViewerStateMachineManager by inject(ViewerStateMachineManager::class.java)
    init {
        println(stateMachine.toString().substring(0,0))
    }
}


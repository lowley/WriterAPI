package io.github.lowley.version2.app.utils

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.google.gson.Gson
import io.github.lowley.common.RichLog
import io.github.lowley.common.searchClient
import io.github.lowley.common.serverSocket
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.app.IAppLogging
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
import io.github.lowley.version2.common.AppEvent.*
import java.net.ServerSocket
import java.net.Socket
import io.github.lowley.version2.common.ErrorMessage
import io.github.lowley.version2.common.toErrorMessage
import io.github.lowley.version2.common.toStateMessage
import io.github.lowley.version2.viewer.utils.ViewerStateMachineManager
import io.github.lowley.version2.app.utils.AndroidAppStates.*
import io.github.lowley.version2.common.AppEvent
import io.github.lowley.version2.viewer.utils.ViewerAppStates
import kotlinx.coroutines.delay
import ru.nsk.kstatemachine.statemachine.BuildingStateMachine

internal class AppStateMachineManager(
    val component: IAppLogging,
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

    suspend fun getStateMachine(scope: CoroutineScope): StateMachine =
        createStateMachine(scope = scope) {

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

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////
    // disconnected //
    //////////////////
    context(scope: BuildingStateMachine)
    private suspend fun disconnectedState() = with(scope) {
        addInitialState(Disconnected)
        {
            onEntry { scope ->
                val result = deviceAPI.reverseAdbPort()
                result.fold(
                    ifLeft = { error ->
                        component.setStateMessage("reverseAdb en erreur".toStateMessage())
                        println("state Disconnected: reverseAdb en erreur")
                        machine.processEvent(GoOnError(error.toErrorMessage()))
                    },
                    ifRight = {
                        val result2 = serverSocket()
                        result2.fold(
                            ifLeft = { error ->
                                println("state Disconnected: serverSocket pas obtenu")
                                component.setStateMessage("serverSocket pas obtenu".toStateMessage())
                                machine.processEvent(GoOnError(error.toErrorMessage()))
                            },
                            ifRight = { seso ->
                                println("state Disconnected: serverSocket obtenu")
                                component.setStateMessage("serverSocket obtenu".toStateMessage())
                                serverSocket = Some(seso)

                                machine.processEvent(StartListening(seso))
                            }
                        )
                    }
                )
            }


            onExit() {}

            transition<StartListening>
            {
                targetState = Listening
                onTriggered { scope ->
                    println("transition StartListening")
                    scope.transition.argument = scope.event.serverSocket
                }
            }

            transition<GoOnError>
            {
                targetState = Error
                onTriggered { scope ->
                    println("transition GoOnError")
                    scope.transition.argument = scope.event.text
                }
            }
        }
    }

    ///////////////
    // listening //
    ///////////////
    context(scope: BuildingStateMachine)
    private suspend fun listeningState() = with(scope) {
        addState(Listening)
        {
            onEntry { scope ->
                val serverSocket = scope.transition.argument as? ServerSocket
                println("state Listening: socket: $serverSocket")

                if (serverSocket != null) {
                    val result = searchClient(serverSocket)
                    result.fold(
                        ifLeft = { error ->
                            println("state Listening: erreur lors recherche client")
                            component.setStateMessage("erreur lors recherche client".toStateMessage())
                            machine.processEvent(GoOnError(error.toErrorMessage()))
                        },
                        ifRight = { so ->
                            println("state Listening: client obtenu")
                            component.setStateMessage("client obtenu".toStateMessage())
                            machine.processEvent(Connect(so))
                        }
                    )
                }
            }

            onExit { }

            transition<Disconnect> {
                targetState = Disconnected
                onTriggered { scope ->
                    println("transition Disconnect")
                }
            }

            transition<Connect> {
                targetState = Connected
                onTriggered { scope ->
                    println("transition Connect")
                    scope.transition.argument = scope.event.socket
                }
            }

            transition<GoOnError> {
                targetState = Error
                onTriggered { scope ->
                    println("transition GoOnError")
                    scope.transition.argument = scope.event.text
                }
            }
        }
    }

    ///////////////
    // connected //
    ///////////////
    context(scope: BuildingStateMachine)
    private suspend fun connectedState() = with(scope) {
        addState(Connected)
        {
            onEntry { scope ->

                //il ne doit pas recevoir mais émettre
                val socket = scope.transition.argument as? Socket
                if (socket != null) {
                    deviceAPI.readClientLines(socket) { line ->
                        try {
                            val event = Gson().fromJson(line, RichLog::class.java)
                            println("state Connecté: reçu log (brut=${event.raw()})")
                            component.emit(event)
                        } catch (ex: Exception) {
                            println("state Connecté: erreur lors parsing de : $line")
                            component.setStateMessage("erreur lors parsing".toStateMessage())
                        }
                    }
                }

                component.setStateMessage("déconnexion initiée par le correspondant".toStateMessage())
                machine.processEvent(Disconnect)
            }

            onExit { }

            transition<Disconnect> {
                targetState = Disconnected
                onTriggered { scope ->
                    val socket = scope.transition.argument as? Socket
                    socket?.close()
                }
            }
        }
    }

    ///////////
    // error //
    ///////////
    context(scope: BuildingStateMachine)
    private suspend fun errorState() = with(scope) {
        addState (Error)
        {

            onEntry { scope ->
                val message = scope.transition.argument as? ErrorMessage
                if (message != null)
                    component.setStateMessage(message.toStateMessage())
            }
            onExit { }

            //manuel
            transition<Disconnect> {
                targetState = Disconnected
                onTriggered { scope ->
                }
            }
        }
    }


    //////////////
    // disabled //
    //////////////
    context(scope: BuildingStateMachine)
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

internal sealed class AndroidAppStates : DefaultState() {
    object Disconnected : AndroidAppStates()
    object Listening : AndroidAppStates()
    object Connected : AndroidAppStates()
    object Error : AndroidAppStates()
}

internal object InitializeAppLogging {
    private val stateMachine: ViewerStateMachineManager by inject(ViewerStateMachineManager::class.java)

    init {
        println(stateMachine.toString().substring(0, 0))
    }
}

package io.github.lowley.engineRoom.boat.utils

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.raise.either
import arrow.core.right
import com.google.gson.Gson
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.ServerMessage
import io.github.lowley.common.socket
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.engineRoom.common.*
import io.github.lowley.engineRoom.common.ErrorMessage
import io.github.lowley.engineRoom.common.NetworkBehavior
import io.github.lowley.engineRoom.common.Success
import io.github.lowley.engineRoom.common.toErrorMessage
import io.github.lowley.engineRoom.common.toStateMessage
import io.github.lowley.engineRoom.boat.SurfaceLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.nsk.kstatemachine.state.addInitialState
import ru.nsk.kstatemachine.state.addState
import ru.nsk.kstatemachine.state.onEntry
import ru.nsk.kstatemachine.state.onExit
import ru.nsk.kstatemachine.state.transition
import ru.nsk.kstatemachine.statemachine.StateMachine
import ru.nsk.kstatemachine.statemachine.createStateMachine
import ru.nsk.kstatemachine.transition.onTriggered
import java.net.Socket
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import lorry.basics.appModule
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import ru.nsk.kstatemachine.statemachine.BuildingStateMachine
import java.io.BufferedWriter

internal class SurfaceStateMachineManager() {
    val component: SurfaceLogging = InitializeViewerLogging.koin.get()
    val deviceAPI: IDeviceAPI = InitializeViewerLogging.koin.get()

    private var HHmmss: String? =
        java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss").format(java.time.LocalTime.now())
        get() = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")
            .format(java.time.LocalTime.now())

    val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var adbComMachine: StateMachine? = null
    var socket: Option<Socket> = None

    init {
        if (adbComMachine == null) {
            coroutineScope.launch() {
                adbComMachine = getStateMachine(coroutineScope)
            }
        }
    }

    suspend fun getStateMachine(scope: CoroutineScope): StateMachine {

        val machine = createStateMachine(scope = scope) {

            with<BuildingStateMachine, Unit>(this@createStateMachine) {
                disconnectedState()
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
    //le viewer est le client
    //il n'a donc pas de ServerSocket ni d'état Listening
    //c'est l'état Disconnected qui se charge de l'obtention du socket
    context(scope: BuildingStateMachine)
    private suspend fun disconnectedState() = with(scope) {
        addInitialState(SurfaceStates.Disconnected) {
            onEntry { scope ->

                val result0 = deviceAPI.reverseAdbPort()
                result0.fold(
                    ifLeft = { error ->
                        println("state Disconnected: reverseAdb en erreur")
                        machine.processEvent(SurfaceGoOnError(error.toErrorMessage()))
                    },
                    ifRight = {
                        var result: Either<AdbError, Socket>? = null

                        do {
                            if (!component.isLoggingEnabledFlow.value) {
                                component.setStateMessage("Désactivation demandée".toStateMessage())
                                machine.processEvent(SurfaceDisable)
                                return@onEntry
                            }

                            component.setStateMessage("Tentative de connexion à $HHmmss".toStateMessage())
                            result = socket(address = "127.0.0.1", port = 7777)

                            delay(3_000)
                        } while (result.isLeft())

                        result.fold(
                            ifLeft = { error ->
//                        component.setStateMessage("Obtention de la connexion impossible".toStateMessage())
//                        println("state Disconnected: connexion à l'app impossible")
//                        machine.processEvent(SurfaceGoOnError(error.toErrorMessage()))
                            },
                            ifRight = {
                                socket = result.getOrNone()
                                component.setStateMessage("Connexion établie".toStateMessage())
                                println("state Disconnected: connexion établie à $HHmmss")
                                machine.processEvent(SurfaceConnect)
                            }
                        )

//                } else {
//                    component.setStateMessage("Désactivation demandée".toStateMessage())
//                    machine.processEvent(SurfaceDisable)
//                }
                    })
            }
            onExit { }

            // transition -> CONNECTED
            transition<SurfaceConnect> {
                targetState = SurfaceStates.Connected
                onTriggered { scope ->
                    println("transition StartListening")
                }
            }

            // transition -> GOONERROR
            transition<SurfaceGoOnError> {
                targetState = SurfaceStates.Error
                onTriggered { scope ->
                    println("transition GoOnError")
                    scope.transition.argument = scope.event.text
                }
            }

            // transition -> DISABLE
            transition<SurfaceDisable> {
                targetState = SurfaceStates.Disabled
                onTriggered { scope ->
                    println("transition Disable")
                }
            }
        }
    }

    ///////////////
    // connected //
    ///////////////
    context(scope: BuildingStateMachine)
    private suspend fun connectedState() = with(scope) {
        addState(SurfaceStates.Connected) {
            onEntry { scope ->
                println("state Connected: socket=$socket")

                if (!component.isLoggingEnabledFlow.value) {
                    component.setStateMessage("Désactivation demandée".toStateMessage())
                    machine.processEvent(SurfaceDisable)
                    return@onEntry
                }

                if (socket == null) {
                    println("state Connected: socket reçue vide")
                    machine.processEvent(SurfaceGoOnError("erreur interne de socket".toErrorMessage()))
                }

                var emitterJob = with(coroutineScope) { createEmitterJob(socket.getOrNull()!!) }
                var receiverJob = with(coroutineScope) { createReceiverJob(socket.getOrNull()!!) }

                ////////////////////////////////
                // gestion emitter & receiver //
                ////////////////////////////////

                try {
                    emitterJob.start()
                    receiverJob.start()
                } catch (ex: CancellationException) {
                    when (ex.message) {
                        NetworkBehavior.Emitter.name -> {
                            try {
                                socket.onSome { it.close() }
                            } catch (_: Exception) {
                            }
                            println("state Connected: erreur d'émission")
                            component.setStateMessage("erreur d'émission d'un message".toStateMessage())
                            machine.processEvent(SurfaceDisconnect)
                        }

                        NetworkBehavior.Receiver.name -> {
                            try {
                                socket.onSome { it.close() }
                            } catch (_: Exception) {
                            }
                            println("state Connected: erreur de réception")
                            component.setStateMessage("erreur de réception de message du Viewer".toStateMessage())
                            machine.processEvent(SurfaceDisconnect)
                        }
                    }
                }

                //???
//                component.setStateMessage("Déconnexion initiée par le correspondant".toStateMessage())
//                machine.processEvent(SurfaceDisconnect)
            }
            onExit { }

            // transition -> DISCONNECT
            transition<SurfaceDisconnect> {
                targetState = SurfaceStates.Disconnected
                onTriggered { scope ->
                    println("transition Disconnect")
                    socket.onSome { it.close() }
                }
            }

            // transition -> DISABLE
            transition<SurfaceDisable> {
                targetState = SurfaceStates.Disabled
                onTriggered { scope ->
                    println("transition Disable")
                }
            }
        }
    }

    ///////////
    // error //
    ///////////
    context(scope: BuildingStateMachine)
    private suspend fun errorState() = with(scope) {
        addState(SurfaceStates.Error) {
            onEntry { scope ->
                if (!component.isLoggingEnabledFlow.value) {
                    component.setStateMessage("Désactivation demandée".toStateMessage())
                    machine.processEvent(SurfaceDisable)
                    return@onEntry
                }

                val message = scope.transition.argument as? ErrorMessage
                if (message != null)
                    component.setStateMessage("Erreur: $message".toStateMessage())
            }
            onExit { }

            //manuel
            // transition -> DISCONNECT
            transition<SurfaceDisconnect> {
                targetState = SurfaceStates.Disconnected
                onTriggered { scope ->
                }
            }

            // transition -> DISABLE
            transition<SurfaceDisable> {
                targetState = SurfaceStates.Disabled
                onTriggered { scope ->
                    println("transition Disable")
                }
            }
        }
    }

    //////////////
    // disabled //
    //////////////
    context(scope: BuildingStateMachine)
    private suspend fun disabledState() = with(scope) {
        addState(SurfaceStates.Disabled) {
            onEntry { scope ->
                val sc = CoroutineScope(Dispatchers.Default + SupervisorJob())
                sc.launch {
                    while (true) {
                        if (component.isLoggingEnabledFlow.value) {
                            machine.processEvent(SurfaceDisconnect)
                            return@launch
                        }

                        delay(500)
                    }
                }
            }

            onExit { }

            // transition -> DISCONNECT
            transition<SurfaceDisconnect> {
                targetState = SurfaceStates.Disconnected
                onTriggered { scope ->
                    println("transition Disconnect")
                }
            }
        }
    }

    context(coroutineScope: CoroutineScope)
    fun createEmitterJob(socket: Socket) = coroutineScope.launch(start = CoroutineStart.LAZY) {
        for (message in component.messagesToBeSentToApp) {

            val emissionRresult = sendMessage(message)
            emissionRresult.fold(
                ifLeft = { error ->
                    throw CancellationException(NetworkBehavior.Emitter.name)
//                    println("state Connected: émission en erreur")>
//                    machine.processEvent(GoOnError("erreur lors d'émission".toErrorMessage()))
                },
                ifRight = {

                }
            )
        }
    }

    context(coroutineScope: CoroutineScope)
    fun createReceiverJob(socket: Socket) = coroutineScope.launch(start = CoroutineStart.LAZY) {
        deviceAPI.readClientLines(socket) { line ->
            try {
                val log = Gson().fromJson(line, RichLog::class.java)
                println("state Connecté: reçu log (brut=${log.raw()})")
                component.sendLogToViewer(log)
            } catch (ex: Exception) {
                throw CancellationException(NetworkBehavior.Receiver.name)

//                println("state Connecté: erreur lors parsing de : $line")
//                component.setStateMessage("erreur lors parsing".toStateMessage())
            }
        }

        component.setStateMessage("Déconnexion initiée par le correspondant".toStateMessage())
        DiveStates.Connected.machine.processEvent(SurfaceDisconnect)
    }

    suspend fun sendMessage(message: ServerMessage): Either<AdbError, Success> =

        withContext(Dispatchers.IO) {
            either {
                socket.fold(
                    ifSome = { socket ->
                        try {
                            val w = ensureWriter(socket).bind()

                            val payload = Gson().toJson(message)
                            w.write(payload)
                            w.write("\n")
                            w.flush()

                            println("message emitted: $socket")

                        } catch (ex: Exception) {
                            ex.printStackTrace()
                            socket.shutdownOutput()
                            socket.close()
                            // fait sortir du either avec un Left
                            raise(AdbError.ExceptionThrown(ex))
                        }
                    },
                    ifEmpty = {
                        raise(AdbError.CommandFailed(1, "socket invalide pour envoi de message"))
                    }
                )

                // Si on arrive ici, tout s'est bien passé
                Success
            }
        }

    private fun ensureWriter(socket: Socket): Either<AdbError, BufferedWriter> = either {
        try {
            val w = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)
            return w.right()
        } catch (ex: Exception) {
            raise(AdbError.ExceptionThrown(ex))
        }
    }
}

internal object InitializeViewerLogging {
    private val app: KoinApplication = koinApplication { modules(appModule) }
    val koin: Koin get() = app.koin

    //on déclenche l'initialisation de la machine
    private val stateMachine: SurfaceStateMachineManager = koin.get()

    init {
        println(stateMachine.toString().substring(0, 0))
    }
}


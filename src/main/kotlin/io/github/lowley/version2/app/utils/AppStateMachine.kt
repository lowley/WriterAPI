package io.github.lowley.version2.app.utils

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.raise.either
import arrow.core.right
import com.google.gson.Gson
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.RichSegment
import io.github.lowley.common.RichText
import io.github.lowley.common.ServerMessage
import io.github.lowley.common.Style
import io.github.lowley.common.TextType
import io.github.lowley.common.searchClient
import io.github.lowley.common.serverSocket
import io.github.lowley.common.socket
import io.github.lowley.receiver.IDeviceAPI
import io.github.lowley.version2.app.AppLogging
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
import io.github.lowley.version2.app.utils.AppStateMachineManager.AndroidAppStates.*
import io.github.lowley.version2.common.AppEvent
import io.github.lowley.version2.common.NetworkBehavior
import io.github.lowley.version2.common.Success
import io.github.lowley.version2.viewer.utils.ViewerAppStates
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import ru.nsk.kstatemachine.statemachine.BuildingStateMachine
import java.io.BufferedWriter
import java.util.Calendar
import kotlinx.coroutines.CancellationException

internal class AppStateMachineManager(
    val component: AppLogging,
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
        }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    //////////////////
    // disconnected //
    //////////////////
    context(scope: BuildingStateMachine)
    private suspend fun disconnectedState() = with(scope) {
        addInitialState(AppStateMachineManager.AndroidAppStates.Disconnected)
        {
            onEntry { scope ->
                val result = deviceAPI.reverseAdbPort()
                result.fold(
                    ifLeft = { error ->
                        println("state Disconnected: reverseAdb en erreur")
                        machine.processEvent(GoOnError(error.toErrorMessage()))
                    },
                    ifRight = {
                        val result2 = serverSocket()
                        result2.fold(
                            ifLeft = { error ->
                                println("state Disconnected: serverSocket pas obtenu")
                                machine.processEvent(GoOnError(error.toErrorMessage()))
                            },
                            ifRight = { seso ->
                                println("state Disconnected: serverSocket obtenu")
                                component.setStateMessage("serverSocket obtenu".toStateMessage())
                                serverSocket = Some(seso)

                                machine.processEvent(Listen)
                            }
                        )
                    }
                )
            }


            onExit() {}

            // transition -> LISTEN
            transition<Listen>
            {
                targetState = Listening
                onTriggered { scope ->
                    println("transition StartListening")
                }
            }

            // transition -> GOONERROR
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
                println("state Listening: socket: $serverSocket")

                serverSocket.fold(
                    ifSome = { serverSocket ->
                        val result = searchClient(serverSocket)
                        result.fold(
                            ifLeft = { error ->
                                println("state Listening: erreur lors recherche client")
                                machine.processEvent(GoOnError(error.toErrorMessage()))
                            },
                            ifRight = { so ->
                                println("state Listening: client obtenu")
                                component.setStateMessage("client obtenu".toStateMessage())
                                machine.processEvent(Connect(so))
                            }
                        )
                    },
                    ifEmpty = {
                        println("state Listening: erreur lors recherche client")
                        machine.processEvent(GoOnError("erreur lors recherche client".toErrorMessage()))
                    }
                )
            }

            onExit { }

            // transition -> CONNECT
            transition<Connect> {
                targetState = Connected
                onTriggered { scope ->
                    println("transition Connect")
                    scope.transition.argument = scope.event.socket
                }
            }

            // transition -> GOONERROR
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

        val coroutineJob = CoroutineScope(Dispatchers.IO + SupervisorJob())

        addState(Connected)
        {
            onEntry { scope ->

                val socket = (scope.transition.argument as? Socket)
                if (socket == null) {
                    println("state Connected: socket reçue vide")
                    machine.processEvent(GoOnError("erreur interne de socket".toErrorMessage()))
                }

                var emitterJob = with(coroutineJob) { createEmitterJob(socket!!) }
                var receiverJob = with(coroutineJob) { createReceiverJob(socket!!) }

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
                                socket?.close()
                            } catch (_: Exception) {
                            }
                            println("state Connected: erreur d'émission")
                            component.setStateMessage("erreur d'émission de log".toStateMessage())
                            machine.processEvent(Listen)
                        }

                        NetworkBehavior.Receiver.name -> {
                            try {
                                socket?.close()
                            } catch (_: Exception) {
                            }
                            println("state Connected: erreur de réception")
                            component.setStateMessage("erreur de réception de message du Viewer".toStateMessage())
                            machine.processEvent(Listen)
                        }
                    }
                }

//                component.setStateMessage("déconnexion initiée par le correspondant".toStateMessage())
//                machine.processEvent(Disconnect)
            }


            onExit { }

            // transition -> DISCONNECT
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
        addState(Error)
        {

            onEntry { scope ->
                val message = scope.transition.argument as? ErrorMessage
                if (message != null)
                    component.setStateMessage(message.toStateMessage())
            }
            onExit { }

            //manuel
            // transition -> DISCONNECT
            transition<Disconnect> {
                targetState = Disconnected
                onTriggered { scope ->
                }
            }
        }
    }

    suspend fun sendRichLog(richLog: RichLog, socket: Socket): Either<AdbError, Success> =

        withContext(Dispatchers.IO) {
            either {
                try {
                    val w = ensureWriter(socket).bind()

                    var specialRichLog: RichLog? = null

                    specialRichLog = RichLog(
                        timestampMillis = Calendar.getInstance().timeInMillis,
                        richText = RichText(
                            richSegments = listOf(
                                RichSegment(
                                    text = TextType("ceci "),
                                    style = Style(bold = false, underline = false),
                                ),
                                RichSegment(
                                    text = TextType("est "),
                                    style = Style(bold = true, underline = false),
                                ),
                                RichSegment(
                                    text = TextType("un "),
                                    style = Style(bold = true, underline = true),
                                ),
                                RichSegment(
                                    text = TextType("castor"),
                                    style = Style(bold = false, underline = true),
                                )
                            )
                        )
                    )

                    //mode normal
                    specialRichLog = null

                    val payload = Gson().toJson(specialRichLog ?: richLog)
                    w.write(payload)
                    w.write("\n")
                    w.flush()

                    println("richLog emitted: ${richLog.raw()}")

                } catch (ex: Exception) {
                    ex.printStackTrace()
                    socket.shutdownOutput()
                    socket.close()
                    // fait sortir du either avec un Left
                    raise(AdbError.ExceptionThrown(ex))
                }

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


    context(coroutineScope: CoroutineScope)
    fun createEmitterJob(socket: Socket) = coroutineScope.launch(start = CoroutineStart.LAZY) {
        for (log in component.logsToBeSentToViewer) {

            val emissionRresult = sendRichLog(log, socket)
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
        deviceAPI.readClientLines(socket!!) { line ->
            try {
                val event = Gson().fromJson(line, ServerMessage::class.java)
                println("state Connecté: reçu message (brut=${event.text})")
                component.sendMessageToApp(event)
            } catch (ex: Exception) {
                throw CancellationException(NetworkBehavior.Receiver.name)

//                println("state Connecté: erreur lors parsing de : $line")
//                component.setStateMessage("erreur lors parsing".toStateMessage())
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
}

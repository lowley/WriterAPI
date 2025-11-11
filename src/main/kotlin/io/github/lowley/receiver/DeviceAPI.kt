package io.github.lowley.receiver

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.google.gson.Gson
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.searchClient
import io.github.lowley.common.serverSocket
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlin.sequences.forEach

class DeviceAPI : IDeviceAPI {

    private var server: ServerSocket? = null
    private var client: Socket? = null

    override fun deviceLogEvents(port: Int): Either<AdbError, Flow<RichLog>> =
        either {
            reverseAdbPort(port).bind()
            println("DeviceAPI: Reverse Adb activé sur port $port")

            server = serverSocket(port).bind()
            if (server == null)
                raise(AdbError.CommandFailed(1, "serveur nul"))

            println("DeviceAPI: Server en écoute sur ${server!!.localPort}")

            flow {
                while (true) {
                    val cli = searchClient(server!!).bind()
                    client = cli
                    println("DeviceAPI: Client de réception de messages connecté sur ${cli.inetAddress}")

                    try {
                        withClientLines(cli).forEach { line ->
                            try {
                                val event = Gson().fromJson(line, RichLog::class.java)
                                emit(event)
                            } catch (ex: Exception) {
                                println("DeviceAPI: json invalide: $line")
                            }
                        }
                        // ici, forEach s’est terminé proprement (readLine == null) → client fermé par le peer
                        println("DeviceAPI: fin de flux (client fermé proprement)")
                    } catch (ex: SocketException) {
                        println("DeviceAPI: SocketException: ${ex.message}")
                        // ici tu peux décider de relancer ou juste laisser la boucle while(true) reprendre
                    } finally {
                        try {
                            cli.close()
                        } catch (_: Exception) {
                        }
                        client = null
                    }

                    // la boucle while(true) repartira sur un nouveau searchClient(server)
                }
            }
        }

    override fun close() {
        server?.close()
        server = null
    }

    override fun reverseAdbPort(port: Int): Either<AdbError, Unit> = try {

        var process: Process? = null
        process = ProcessBuilder("adb", "reverse", "tcp:$port", "tcp:$port")
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process!!.waitFor()

        if (exitCode != 0)
            AdbError.CommandFailed(exitCode, output).left()
        else
            Unit.right()

    } catch (ex: Exception) {
        ex.printStackTrace()
        AdbError.ExceptionThrown(ex).left()
    }

    private fun withClientLines(client: Socket): Sequence<String> {

        val reader = client.getInputStream().bufferedReader(Charsets.UTF_8)
        return reader.lineSequence()
    }

    override fun readClientLines(client: Socket, onLineReceived: suspend (line: String) -> Unit) {
        client.getInputStream()
            .bufferedReader(Charsets.UTF_8)
            .use { reader ->
                for (line in reader.lineSequence()) {
                    // traite line
                }
            }
    }
}
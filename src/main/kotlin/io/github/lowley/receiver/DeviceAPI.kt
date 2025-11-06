package io.github.lowley.receiver

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.google.gson.Gson
import io.github.lowley.common.RichLogEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import kotlin.sequences.forEach

class DeviceAPI : IDeviceAPI {

    private var client: Socket? = null

    override fun deviceLogEvents(port: Int): Either<AdbError, Flow<RichLogEvent>> =
        either {
            flow {
                reverseAdbPort(port).bind()
                println("Reverse Adb activé sur port $port")

                val server = serverSocket(port).bind()
                println("Server en écoute sur ${server!!.localPort}")

                while (true) {
                    client = searchClient(server).bind()
                    println("Client de réception de messages connecté sur ${client!!.inetAddress}")

                    withClientLines(client).forEach { line ->
                        try {
                            val event = Gson().fromJson(line, RichLogEvent::class.java)
                            emit(event)
                        } catch (ex: Exception) {
                            println("json invalide: $line")
                        }
                    }
                }
            }
        }


    private fun withClientLines(client: Socket?): Sequence<String> {

        val result = client.use { cli ->
            if (cli == null)
                return@use sequenceOf("")
            if (client == null)
                return@use emptySequence()

            val reader = client.getInputStream().bufferedReader(Charsets.UTF_8)
            return@use reader.lineSequence()
        }

        return result
    }

    private fun reverseAdbPort(port: Int): Either<AdbError, Unit> = try {

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

    private fun serverSocket(port: Int): Either<AdbError, ServerSocket> = try {
        val result = ServerSocket(port)
        result.right()

    } catch (ex: IOException) {
        ex.printStackTrace()
        AdbError.ExceptionThrown(ex).left()
    }

    private fun searchClient(server: ServerSocket): Either<AdbError, Socket> = try {
        val client = server.accept()
        client.right()

    } catch (ex: IOException) {
        ex.printStackTrace()
        AdbError.ExceptionThrown(ex).left()
    }

    sealed interface AdbError {
        data class CommandFailed(val exitCode: Int, val output: String) : AdbError
        data class ExceptionThrown(val throwable: Throwable) : AdbError
    }
}
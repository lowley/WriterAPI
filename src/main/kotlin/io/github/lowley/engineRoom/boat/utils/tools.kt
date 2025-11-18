package io.github.lowley.engineRoom.boat.utils

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.google.gson.Gson
import io.github.lowley.common.AdbError
import io.github.lowley.common.ServerMessage
import io.github.lowley.engineRoom.common.Success
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.net.Socket

suspend internal fun SurfaceStateMachineManager.sendMessage(message: ServerMessage): Either<AdbError, Success> =

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

internal fun reverseAdbPort(port: Int = 7777): Either<AdbError, Unit> = try {

    var process: Process? = null
    process = ProcessBuilder("adb", "forward", "tcp:$port", "tcp:$port")
//        process = ProcessBuilder("adb", "reverse", "tcp:$port", "tcp:$port")
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


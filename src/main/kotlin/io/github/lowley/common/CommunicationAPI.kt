package io.github.lowley.common

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket

fun serverSocket(port: Int = 7777): Either<AdbError, ServerSocket> = try {
    val result = ServerSocket(port)
    result.right()

} catch (ex: IOException) {
    ex.printStackTrace()
    AdbError.ExceptionThrown(ex).left()
}

fun searchClient(server: ServerSocket): Either<AdbError, Socket> = try {
    val client = server.accept()
    client.right()

} catch (ex: IOException) {
    ex.printStackTrace()
    AdbError.ExceptionThrown(ex).left()
}

fun socket(port: Int, address: String = "127.0.0.1"): Either<AdbError, Socket> = try {
    val result = Socket(address, port)
    result.right()

}
catch (ex: Exception) {
//    println("socket establishment in failure")
    AdbError.ExceptionThrown(ex).left()
}

sealed interface AdbError {
    data class CommandFailed(val exitCode: Int, val output: String) : AdbError
    data class ExceptionThrown(val throwable: Throwable) : AdbError
}



internal fun readClientLines(
    client: Socket,
    scope: CoroutineScope,
    onLineReceived: suspend (line: String) -> Unit
) {

    client.getInputStream()
        .bufferedReader(Charsets.UTF_8)
        .use { reader ->
            for (line in reader.lineSequence()) {
                // traite line
                scope.launch {
                    onLineReceived(line)
                }
            }
        }
}
package io.github.lowley.common

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.net.UnknownHostException

fun serverSocket(port: Int): Either<AdbError, ServerSocket> = try {
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
catch (ex: IOException) {
    ex.printStackTrace()
    AdbError.ExceptionThrown(ex).left()
}
catch (ex: UnknownHostException) {
    ex.printStackTrace()
    AdbError.ExceptionThrown(ex).left()
}



sealed interface AdbError {
    data class CommandFailed(val exitCode: Int, val output: String) : AdbError
    data class ExceptionThrown(val throwable: Throwable) : AdbError
}
package io.github.lowley.emitter

import arrow.core.Either
import arrow.core.computations.ResultEffect.bind
import arrow.core.computations.either
import arrow.core.raise.either
import arrow.core.right
import com.google.gson.Gson
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.net.Socket

class LoggerCommunicationAPI : ILoggerCommunicationAPI {

    private var socket: Socket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var writer: BufferedWriter? = null

    suspend override fun sendRichLog(
        richLog: RichLog,
        port: Int
    ): Either<AdbError, ILoggerCommunicationAPI.Success> =
        withContext(Dispatchers.IO) {
            either {
                try {
                    val w = ensureWriter(port).bind()

                    val payload = Gson().toJson(richLog)
                    w.write(payload)
                    w.write("\n")
                    w.flush()

                } catch (ex: Exception) {
                    ex.printStackTrace()
                    socket?.close()
                    socket = null
                    // fait sortir du either avec un Left
                    raise(AdbError.ExceptionThrown(ex))
                }

                // Si on arrive ici, tout s'est bien passé
                ILoggerCommunicationAPI.Success
            }
        }

    private fun ensureWriter(port: Int): Either<AdbError, BufferedWriter> = either {
        val existing = writer
        if (existing != null) return existing.right()

        val s = socket(port).bind()
        socket = s
        println("Socket ready to emit on ${s.localPort}")

        val w = s.getOutputStream().bufferedWriter(Charsets.UTF_8)
        writer = w
        return w.right()
    }

}
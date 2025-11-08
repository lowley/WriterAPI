package io.github.lowley.emitter

import arrow.core.Either
import arrow.core.raise.either
import com.google.gson.Gson
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.net.Socket

class LoggerCommunicationAPI : ILoggerCommunicationAPI {

    private var socket: Socket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend override fun sendRichLog(
        richLog: RichLog,
        port: Int
    ): Either<AdbError, ILoggerCommunicationAPI.Success> =
        withContext(Dispatchers.IO) {
            either {

                socket = socket(port).bind()
                println("Socket prêt à émettre sur ${socket?.localPort}")

                try {
                    if (socket == null)
                        raise(AdbError.CommandFailed(1,"socket nul"))

                    val writer = socket!!.getOutputStream().bufferedWriter(Charsets.UTF_8)

                    val payload = Gson().toJson(richLog)
                    writer.write(payload)
                    writer.write("\n")
                    writer.flush()

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
}
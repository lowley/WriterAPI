package io.github.lowley.emitter

import arrow.core.Either
import arrow.core.raise.either
import com.google.gson.Gson
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.searchClient
import io.github.lowley.common.socket
import io.github.lowley.receiver.DeviceAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Socket

class LoggerCommunicationAPI : ILoggerCommunicationAPI {

    private var client: Socket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend override fun sendRichLog(
        richLog: RichLog,
        port: Int
    ): Either<AdbError, ILoggerCommunicationAPI.Success> = either {
        withContext(Dispatchers.IO) {
            val socket = socket(port).bind()
            println("Socket prêt à émettre sur ${socket.localPort}")

            try {
                socket.use { socket ->
                    val writer = socket.getOutputStream().bufferedWriter(Charsets.UTF_8)

                    val payload = Gson().toJson(richLog)
                    writer.write(payload)
                    writer.write("\n")
                    writer.flush()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                // fait sortir du either avec un Left
                raise(AdbError.ExceptionThrown(ex))
            }
        }

        // Si on arrive ici, tout s'est bien passé
        ILoggerCommunicationAPI.Success
    }
}
package io.github.lowley.emitter

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import com.google.gson.Gson
import io.github.lowley.common.AdbError
import io.github.lowley.common.RichLog
import io.github.lowley.common.RichSegment
import io.github.lowley.common.RichText
import io.github.lowley.common.Style
import io.github.lowley.common.TextType
import io.github.lowley.common.socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.net.Socket
import java.util.Calendar

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
//                    specialRichLog = null

                    val payload = Gson().toJson(specialRichLog ?: richLog)
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
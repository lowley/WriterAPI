package io.github.lowley.version2.common

@JvmInline
value class StateMessage(val message: String){
    companion object {
        val EMPTY: StateMessage = StateMessage("EMPTY")
    }
}


internal fun String.toStateMessage(): StateMessage = StateMessage(this)

object Success

enum class NetworkBehavior(text: String){
    Emitter("Emitter"),
    Receiver("Receiver"),
}
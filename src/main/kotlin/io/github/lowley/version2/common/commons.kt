package io.github.lowley.version2.common

@JvmInline
value class StateMessage(val message: String){
    companion object {
        val EMPTY: StateMessage = StateMessage("EMPTY")
    }
}


fun String.toStateMessage(): StateMessage = StateMessage(this)
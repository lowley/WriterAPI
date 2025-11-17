package io.github.lowley.version2.common

import ru.nsk.kstatemachine.event.Event
import ru.nsk.kstatemachine.state.DefaultState

////////////
// common //
////////////
internal sealed class Events {
    object Listen : Events(), Event
    object Disconnect : Events(), Event
    object Connect : Events(), Event
    data class GoOnError(val text: ErrorMessage) : Events(), Event
    object Disable : Events(), Event
}


//////////
// boat //
//////////
internal typealias SurfaceConnect    = Events.Connect
internal typealias SurfaceDisconnect = Events.Disconnect
internal typealias SurfaceGoOnError  = Events.GoOnError
internal typealias SurfaceDisable  = Events.Disable


internal sealed class SurfaceStates : DefaultState() {
    object Disabled : SurfaceStates()
    object Disconnected : SurfaceStates()
    object Listening : SurfaceStates()
    object Connected : SurfaceStates()
    object Error : SurfaceStates()
}

///////////////
// submarine //
///////////////

internal typealias DiveListen     = Events.Listen
internal typealias DiveConnect    = Events.Connect
internal typealias DiveDisconnect = Events.Disconnect
internal typealias DiveGoOnError  = Events.GoOnError

internal sealed class DiveStates : DefaultState() {
    object Disconnected : DiveStates()
    object Listening : DiveStates()
    object Connected : DiveStates()
    object Error : DiveStates()
}

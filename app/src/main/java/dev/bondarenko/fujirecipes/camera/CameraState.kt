package dev.bondarenko.fujirecipes.camera

/**
 * What the app knows about the camera, as one value.
 *
 * `PRD.md` §8.4's states, and §7.2's indicator is a rendering of exactly these six. Held by
 * [CameraController] above the nav graph, so a connection survives opening a recipe — losing
 * the camera because you navigated would be maddening.
 */
sealed interface CameraState {

    /**
     * This device has no USB host support and never will.
     *
     * Not an error, and it offers no retry: a phone that cannot host will not start being able
     * to, and a Retry button here is a lie someone acts on (P5).
     */
    data object NoUsbHost : CameraState

    /** Host support exists; no camera is connected. Whether one is *attached* is separate. */
    data object Disconnected : CameraState

    /** Permission is being asked for, or the session is opening. */
    data object Connecting : CameraState

    data class Connected(val identity: ModelIdentity) : CameraState

    /** Held during a write (FEAT-006). [current] is the property being written right now. */
    data class Writing(
        val slot: Int,
        val done: Int,
        val total: Int,
        val current: String,
    ) : CameraState

    /**
     * A failure, in words that name a remedy.
     *
     * [ptpCode] is the camera's own response code when it had one, so the sheet can say
     * `DeviceBusy` rather than only "connection failed" (P5).
     */
    data class Error(val message: String, val ptpCode: Int? = null) : CameraState
}

/** Whether a write can be offered at all — connected, to a body with custom slots (WR-01). */
val CameraState.canWrite: Boolean
    get() = this is CameraState.Connected && identity.writable

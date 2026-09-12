package dev.bondarenko.fujirecipes.data.lab

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The lab's work in progress, held above the nav graph.
 *
 * Bottom-bar navigation destroys a destination's ViewModel, and the lab's state is expensive in
 * a way an editor's is not: behind it sits a RAF the camera has already been given. Losing that
 * because someone checked the photo list would mean another multi-megabyte upload, so what the
 * lab is doing lives here — beside `CameraController`, for the same reason and with the same
 * lifetime (`architecture.md` §3).
 *
 * **Process death starts the lab over, by design.** `RawDevelopmentCache` empties itself on
 * construction, so a relaunched app has no cached RAF to point a restored session at; a
 * snapshot naming a file that is no longer there would be a worse answer than a clean start.
 */
class RawLabWorkspace {

    private val _state = MutableStateFlow(RawLabState())
    val state: StateFlow<RawLabState> = _state.asStateFlow()

    fun update(transform: (RawLabState) -> RawLabState) {
        _state.update(transform)
    }

    /** Reads the current value without collecting — for decisions taken mid-render. */
    val current: RawLabState get() = _state.value

    fun reset() {
        _state.value = RawLabState()
    }
}

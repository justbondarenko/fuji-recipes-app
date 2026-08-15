package dev.bondarenko.fujirecipes.ui.camera

import androidx.annotation.StringRes
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.CameraState

/**
 * What the camera chip looks like in each of `PRD.md` §7.2's six states.
 *
 * A plain function rather than logic inside the composable, mirroring
 * `fuji-recipes-book/src/utils/camera-chip.ts` @ `0c17106`: behaviour worth testing belongs
 * where a unit test can reach it, and `CameraChip` renders what this returns and adds nothing
 * of its own.
 *
 * **Colour is never the only signal.** Every state has its own icon and its own words, because
 * a green pill and a grey pill are the same pill to a colour-blind reader and to anyone
 * outdoors — which is where this app is used (`architecture.md` C4).
 */

enum class CameraChipIcon { USB, INFO, CONNECTING, CAMERA, WARNING }

/**
 * The colour role, named by meaning rather than by colour.
 *
 * The composable maps these to the scheme, so the palette can change without this file
 * knowing.
 */
enum class CameraChipTone { NEUTRAL, MUTED, WAITING, READY, ALERT }

data class CameraChipLook(
    /** Used unless [modelLabel] is set. */
    @param:StringRes val labelRes: Int,
    /**
     * The body's own name, for the connected state.
     *
     * `PRD.md` §7.2 wants the model rather than the word "Connected": "X100VI" says more, and
     * it is also the fastest way to notice the app has misidentified the body.
     */
    val modelLabel: String? = null,
    val icon: CameraChipIcon,
    val spinning: Boolean = false,
    val tone: CameraChipTone,
    /** The write's progress, 0f..1f, when one is running. */
    val progress: Float? = null,
)

fun cameraChipLook(state: CameraState): CameraChipLook = when (state) {
    // Not an error. This phone will never do it, and offering a retry would be a lie someone
    // acts on (P5); the sheet explains why.
    CameraState.NoUsbHost -> CameraChipLook(
        labelRes = R.string.camera_chip_no_usb,
        icon = CameraChipIcon.INFO,
        tone = CameraChipTone.NEUTRAL,
    )

    CameraState.Disconnected -> CameraChipLook(
        labelRes = R.string.camera_chip_connect,
        icon = CameraChipIcon.USB,
        tone = CameraChipTone.MUTED,
    )

    CameraState.Connecting -> CameraChipLook(
        labelRes = R.string.camera_chip_connecting,
        icon = CameraChipIcon.CONNECTING,
        spinning = true,
        tone = CameraChipTone.WAITING,
    )

    is CameraState.Connected -> CameraChipLook(
        labelRes = R.string.camera_chip_connected,
        modelLabel = state.identity.model.ifBlank { null },
        icon = CameraChipIcon.CAMERA,
        tone = CameraChipTone.READY,
    )

    is CameraState.Writing -> CameraChipLook(
        labelRes = R.string.camera_chip_writing,
        icon = CameraChipIcon.CAMERA,
        tone = CameraChipTone.READY,
        // The bar is this state's distinguishing signal, and it is the visual evidence that
        // something is genuinely happening on the wire (`PRD.md` §5.4).
        progress = if (state.total == 0) 0f else state.done.toFloat() / state.total,
    )

    is CameraState.Error -> CameraChipLook(
        labelRes = R.string.camera_chip_error,
        icon = CameraChipIcon.WARNING,
        tone = CameraChipTone.ALERT,
    )
}

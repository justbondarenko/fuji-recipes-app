package dev.bondarenko.fujirecipes.camera.usb

import dev.bondarenko.fujirecipes.camera.ptp.PtpFramingError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import java.io.File
import kotlinx.serialization.json.JsonObject

/**
 * One RAF, many renders.
 *
 * The lab's whole shape depends on not re-sending tens of megabytes every time a slider moves,
 * so this remembers which file the camera is already holding and skips straight to patching the
 * profile. That the camera *will* convert repeatedly from one upload is
 * `specs/plans/raw-development-lab.md`'s Gate A: the fallback if it does not is a body that
 * refuses or misbehaves on the second conversion, which lands here as an ordinary render
 * failure and, for a transport-level failure, as a re-upload on the next attempt.
 *
 * One of these belongs to one [PtpSession]. Nothing here survives the cable: a new session
 * means a camera that is holding nothing, so `CameraController` builds a new instance rather
 * than clearing this one.
 */
class RawLabSession {

    /** Identifies the loaded file, not merely its path: a re-imported RAF is a new upload. */
    private var loadedKey: String? = null

    val isLoaded: Boolean get() = loadedKey != null

    /** The camera is no longer holding what we think it is. */
    fun invalidate() {
        loadedKey = null
    }

    /**
     * Renders [settings] against [raf], uploading it first only if it is not already loaded.
     *
     * A failure after a successful upload keeps the load: a refused setting or a render that
     * timed out says nothing about the file the camera is holding, and throwing that work away
     * would cost another full transfer to learn the same thing. A framing or timeout error is
     * different — the session is unusable, and whatever the camera holds is unknowable — so the
     * next call uploads again.
     */
    fun render(
        session: PtpSession,
        raf: File,
        settings: JsonObject,
        output: File,
        quality: RawRenderQuality = RawRenderQuality.FULL,
        onStage: (RawDevelopmentStage) -> Unit = {},
        renderTimeoutMs: Long = 30_000,
    ): RawDevelopmentResult = withLoaded(session, raf, onStage) {
        val patch = applyRawSettings(session, settings, onStage)
        convertAndFetch(session, patch, output, onStage, renderTimeoutMs, quality)
    }

    /**
     * The camera's native profile for [raf], uploading it first if needed.
     *
     * This is the calibration path: a body with no verified adapter can still be asked what its
     * `0xD185` block looks like, which is the fixture an adapter is written from.
     */
    fun profile(
        session: PtpSession,
        raf: File,
        onStage: (RawDevelopmentStage) -> Unit = {},
    ): ByteArray = withLoaded(session, raf, onStage) { readRawProfile(session) }

    private fun <T> withLoaded(
        session: PtpSession,
        raf: File,
        onStage: (RawDevelopmentStage) -> Unit,
        block: () -> T,
    ): T {
        val key = keyFor(raf)
        if (loadedKey != key) {
            loadedKey = null
            onStage(RawDevelopmentStage.Preparing)
            uploadRaf(session, raf) { written, total ->
                onStage(RawDevelopmentStage.Uploading(written, total))
            }
            loadedKey = key
        }
        return try {
            block()
        } catch (error: Exception) {
            if (error is PtpFramingError || error is PtpTimeoutError) invalidate()
            throw error
        }
    }

    private fun keyFor(raf: File): String =
        "${raf.absolutePath}:${raf.length()}:${raf.lastModified()}"
}

package dev.bondarenko.fujirecipes.camera.usb

/** A claimed interface cannot be used until its required alternate setting is active. */
internal fun <T> claimAndActivateUsbInterface(
    candidates: List<T>,
    claim: (T) -> Boolean,
    activate: (T) -> Boolean,
    release: (T) -> Unit,
): T? {
    for (candidate in candidates) {
        if (!claim(candidate)) continue
        try {
            if (activate(candidate)) return candidate
        } catch (error: Exception) {
            release(candidate)
            throw error
        }
        release(candidate)
    }
    return null
}

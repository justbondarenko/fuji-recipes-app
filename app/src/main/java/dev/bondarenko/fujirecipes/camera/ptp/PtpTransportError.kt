package dev.bondarenko.fujirecipes.camera.ptp

/** An unfinished or malformed USB transaction makes the session unsafe to reuse. */
sealed class PtpTransportError(message: String, cause: Throwable? = null) : Exception(message, cause)

enum class UsbTransferDirection { READ, WRITE }

/**
 * Android bulkTransfer returns a negative count for many failures, not just timeouts.
 * Preserve the measured wait and transfer details rather than inventing a cause.
 */
class PtpIoError(
    val direction: UsbTransferDirection,
    val endpointAddress: Int,
    val requestedBytes: Int,
    val timeoutMs: Int,
    val elapsedMs: Long,
    val operation: Int? = null,
    val property: Int? = null,
    cause: Throwable? = null,
) : PtpTransportError(
    "USB ${direction.name.lowercase()} failed after $elapsedMs ms " +
        "(limit $timeoutMs ms, endpoint 0x${endpointAddress.toString(16)}, $requestedBytes bytes). " +
        "Reconnect the camera before trying again." + ptpOperationContext(operation, property),
    cause,
) {
    internal fun withOperation(operation: Int, property: Int?): PtpIoError = PtpIoError(
        direction, endpointAddress, requestedBytes, timeoutMs, elapsedMs, operation, property, this,
    )
}

internal fun ptpOperationContext(operation: Int?, property: Int?): String =
    if (operation != null) {
        " (Operation 0x${operation.toString(16)}" +
            (property?.let { ", property 0x${it.toString(16)}" } ?: "") + ")"
    } else ""

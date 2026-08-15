package dev.bondarenko.fujirecipes.camera

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpFramingError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.usb.InterruptReason
import dev.bondarenko.fujirecipes.camera.usb.UsbBulkChannel
import dev.bondarenko.fujirecipes.camera.usb.UsbConnectError
import dev.bondarenko.fujirecipes.camera.usb.WriteInterrupted
import dev.bondarenko.fujirecipes.camera.usb.WriteOutcome
import dev.bondarenko.fujirecipes.camera.usb.executeWritePlan
import dev.bondarenko.fujirecipes.camera.usb.partialWriteWarning
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The camera connection, as one long-lived object.
 *
 * Held by `AppContainer` above the nav graph (`architecture.md` §3), so opening a recipe does
 * not drop the connection. Everything I/O runs on [Dispatchers.IO]; the state is a
 * [StateFlow] the shell's chip and the write sheet both read.
 *
 * The protocol itself is not here. This owns the *lifecycle* — finding the device, asking for
 * permission, opening and closing the session, and noticing the cable leaving — and delegates
 * every byte to `camera/ptp` and `camera/usb`.
 */
class CameraController(
    context: Context,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {

    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    private val hasHostSupport =
        appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

    private val _state = MutableStateFlow<CameraState>(
        if (hasHostSupport) CameraState.Disconnected else CameraState.NoUsbHost,
    )
    val state: StateFlow<CameraState> = _state.asStateFlow()

    /** One connection at a time; `connect` can arrive from the chip and the attach intent. */
    private val lock = Mutex()

    private var session: PtpSession? = null

    /** Whether a camera is on the bus at all, whatever the connection state. */
    val isCameraAttached: Boolean
        get() = hasHostSupport && UsbBulkChannel.findCamera(usbManager) != null

    init {
        if (hasHostSupport) registerReceivers()
    }

    // ─── Connecting ─────────────────────────────────────────────────────────

    /**
     * Connects to the attached camera, asking for USB permission first if it is not held.
     *
     * Safe to call in any state; a call while already connected is a no-op rather than a
     * reconnection, because tapping the chip twice should not drop a working session.
     */
    fun connect() {
        if (!hasHostSupport) return
        scope.launch { connectNow(UsbBulkChannel.findCamera(usbManager)) }
    }

    /**
     * The attach-intent path (`PRD.md` §8.2).
     *
     * Launching this way grants permission implicitly for that connection, so there is no
     * dialog: plug the camera in, the app opens, the chip already names the body. This is the
     * feature that justifies going native.
     */
    fun onDeviceAttached(device: UsbDevice?) {
        if (!hasHostSupport) return
        scope.launch { connectNow(device ?: UsbBulkChannel.findCamera(usbManager)) }
    }

    private suspend fun connectNow(device: UsbDevice?) = lock.withLock {
        if (_state.value is CameraState.Connected || _state.value is CameraState.Writing) return

        if (device == null) {
            _state.value = CameraState.Error(
                "No camera is attached. Connect it with a USB-C cable, and set the camera's " +
                    "USB mode to the RAW-conversion/backup mode.",
            )
            return
        }

        _state.value = CameraState.Connecting

        if (!usbManager.hasPermission(device)) {
            requestPermission(device)
            // The broadcast receiver picks it up from here, in either direction.
            return
        }

        openSession(device)
    }

    /**
     * The intent must be **explicit** — Android 14 refuses a mutable implicit `PendingIntent`
     * outright — and **mutable**, because the system fills in the device and the grant result.
     * The grant is re-checked against `hasPermission` regardless, so a stripped extra cannot
     * produce a false positive.
     */
    private fun requestPermission(device: UsbDevice) {
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
        val pending = PendingIntent.getBroadcast(
            appContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        usbManager.requestPermission(device, pending)
    }

    private fun openSession(device: UsbDevice) {
        try {
            val channel = UsbBulkChannel.open(usbManager, device)
            val opened = PtpSession(PtpTransport(channel))
            val info = opened.open()

            session = opened
            _state.value = CameraState.Connected(CameraModels.identify(info.model))
        } catch (error: Exception) {
            session = null
            _state.value = error.toCameraError()
        }
    }

    // ─── Disconnecting ──────────────────────────────────────────────────────

    fun disconnect() {
        scope.launch { lock.withLock { closeSession(CameraState.Disconnected) } }
    }

    private fun closeSession(next: CameraState) {
        session?.let { runCatching { it.close() } }
        session = null
        _state.value = next
    }

    // ─── Broadcasts ─────────────────────────────────────────────────────────

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> scope.launch {
                    lock.withLock {
                        val device = UsbBulkChannel.findCamera(usbManager)
                        when {
                            device == null -> _state.value = CameraState.Disconnected
                            // The extra is not trusted: the grant is re-read from the manager,
                            // which is the authority and cannot be stripped by a stray intent.
                            !usbManager.hasPermission(device) -> _state.value = CameraState.Error(
                                "USB permission was refused, so the app cannot reach the " +
                                    "camera. Tap to ask again.",
                            )

                            else -> openSession(device)
                        }
                    }
                }

                // A camera plugged in while the app is already open. The attach *intent* path
                // is `onDeviceAttached`; this is the same event arriving at a running app.
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    if (_state.value is CameraState.Disconnected) connect()
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> scope.launch {
                    lock.withLock {
                        // A write in progress is a partial slot, and the sheet says so rather
                        // than the state quietly reading "disconnected" as if nothing was
                        // underway.
                        val next = if (_state.value is CameraState.Writing) {
                            CameraState.Error(
                                "The camera was unplugged during the write. The slot may be " +
                                    "partly written.",
                            )
                        } else {
                            CameraState.Disconnected
                        }
                        closeSession(next)
                    }
                }
            }
        }
    }

    // ─── Writing (FEAT-006) ─────────────────────────────────────────────────

    /**
     * Executes a plan against the open session.
     *
     * Serialised behind the same lock as connecting, so a detach arriving mid-write cannot
     * tear the session down underneath the executor — it waits, and finds the state already
     * moved on.
     *
     * The state goes to [CameraState.Writing] for the duration and back to
     * [CameraState.Connected] afterwards, so the chip's progress bar is driven by the same
     * value everything else reads.
     */
    suspend fun write(
        plan: WritePlan,
        isCancelled: () -> Boolean = { false },
    ): WriteResult = lock.withLock {
        val open = session
        val connected = _state.value as? CameraState.Connected

        if (open == null || connected == null) {
            return WriteResult.Failure(
                "The camera is not connected, so there is nothing to write to.",
            )
        }

        /**
         * A partial wake lock for the duration of the write, and no longer.
         *
         * `PRD.md` §8.4: writes take seconds, and a screen timeout with doze arriving
         * mid-transfer is a bad way to leave a slot half-written. No foreground service — the
         * operation is short and the user started it.
         */
        val wakeLock = (appContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)

        return try {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
            _state.value = CameraState.Writing(plan.slot, 0, plan.total, "")

            val outcome = withContext(Dispatchers.IO) {
                executeWritePlan(
                    session = open,
                    plan = plan,
                    onProgress = { progress ->
                        _state.value = CameraState.Writing(
                            slot = plan.slot,
                            done = progress.done,
                            total = progress.total,
                            current = progress.label,
                        )
                    },
                    isCancelled = isCancelled,
                )
            }

            _state.value = connected
            WriteResult.Success(outcome)
        } catch (error: WriteInterrupted) {
            // A disconnect has already been handled by the detach receiver, which left an
            // Error state saying so. Anything else returns to connected: the camera is still
            // there, and the sheet reports what happened.
            if (error.reason != InterruptReason.DISCONNECTED) _state.value = connected

            WriteResult.Failure(
                message = error.message.orEmpty(),
                warning = partialWriteWarning(error.outcome),
                outcome = error.outcome,
            )
        } catch (error: Exception) {
            _state.value = connected
            WriteResult.Failure(error.toCameraError().message)
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    /** The open session, or null. */
    internal fun openSessionOrNull(): PtpSession? = session

    companion object {
        /** Private to this app: the filter is registered `RECEIVER_NOT_EXPORTED`. */
        const val ACTION_USB_PERMISSION = "dev.bondarenko.fujirecipes.USB_PERMISSION"

        private const val WAKE_LOCK_TAG = "fujirecipes:camera-write"

        /**
         * A backstop, not a budget. A write is seconds; this only guarantees the lock is not
         * left held for ever if something goes wrong in a way `finally` cannot see.
         */
        private const val WAKE_LOCK_TIMEOUT_MS = 2 * 60 * 1000L
    }
}

/** What a write did, as the sheet needs to render it. */
sealed interface WriteResult {
    data class Success(val outcome: WriteOutcome) : WriteResult

    data class Failure(
        val message: String,
        /** What the slot now holds, when anything was written before this stopped. */
        val warning: String? = null,
        val outcome: WriteOutcome? = null,
    ) : WriteResult
}

/**
 * A failure, in words that name the remedy — `coding-standards.md` P5.
 *
 * Specifically forbidden: rendering a busy interface, a wrong USB mode and a timeout as the
 * same message. They have three different remedies.
 */
internal fun Throwable.toCameraError(): CameraState.Error = when (this) {
    is UsbConnectError -> CameraState.Error(message.orEmpty())

    is PtpError -> CameraState.Error(message.orEmpty(), ptpCode = code)

    is PtpTimeoutError -> CameraState.Error(message.orEmpty())

    is PtpFramingError -> CameraState.Error(
        "The camera answered something this app could not read: ${message.orEmpty()}",
    )

    else -> CameraState.Error(
        "The camera connection failed: ${message ?: this::class.simpleName}",
    )
}

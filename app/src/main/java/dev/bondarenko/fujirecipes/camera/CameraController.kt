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
import androidx.core.content.IntentCompat
import dev.bondarenko.fujirecipes.camera.plan.SlotNameReading
import dev.bondarenko.fujirecipes.camera.plan.WritePlan
import dev.bondarenko.fujirecipes.camera.ptp.PtpError
import dev.bondarenko.fujirecipes.camera.ptp.PtpFramingError
import dev.bondarenko.fujirecipes.camera.ptp.PtpSession
import dev.bondarenko.fujirecipes.camera.ptp.PtpTimeoutError
import dev.bondarenko.fujirecipes.camera.ptp.PtpTransport
import dev.bondarenko.fujirecipes.camera.usb.FUJI_VENDOR_ID
import dev.bondarenko.fujirecipes.camera.usb.InterruptReason
import dev.bondarenko.fujirecipes.camera.usb.UsbBulkChannel
import dev.bondarenko.fujirecipes.camera.usb.UsbConnectError
import dev.bondarenko.fujirecipes.camera.usb.WriteInterrupted
import dev.bondarenko.fujirecipes.camera.usb.WriteOutcome
import dev.bondarenko.fujirecipes.camera.usb.executeWritePlan
import dev.bondarenko.fujirecipes.camera.usb.partialWriteWarning
import dev.bondarenko.fujirecipes.camera.usb.SlotRecipe
import dev.bondarenko.fujirecipes.camera.usb.readSlotNames
import dev.bondarenko.fujirecipes.camera.usb.readSlotRecipe
import dev.bondarenko.fujirecipes.camera.usb.readSlotRecipes
import dev.bondarenko.fujirecipes.camera.usb.readCameraDetails
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.camera.usb.readUsbMode
import dev.bondarenko.fujirecipes.camera.usb.readCameraReport
import dev.bondarenko.fujirecipes.camera.usb.CameraMediaObject
import dev.bondarenko.fujirecipes.camera.usb.downloadCameraJpeg
import dev.bondarenko.fujirecipes.camera.usb.downloadCameraRaf
import dev.bondarenko.fujirecipes.camera.usb.listCameraFiles
import dev.bondarenko.fujirecipes.camera.usb.listCameraJpegs
import dev.bondarenko.fujirecipes.camera.usb.readCameraThumbnail
import dev.bondarenko.fujirecipes.core.store.CameraMediaCache
import java.io.File
import java.io.OutputStream
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentResult
import dev.bondarenko.fujirecipes.camera.usb.RawDevelopmentStage
import dev.bondarenko.fujirecipes.camera.usb.RawLabSession
import dev.bondarenko.fujirecipes.camera.usb.RawRenderQuality
import kotlinx.serialization.json.JsonObject
import dev.bondarenko.fujirecipes.camera.plan.CameraReport
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

    /**
     * What the camera is holding for the lab, for as long as this session lasts.
     *
     * Tied to the [PtpSession] rather than to the screen: a body that has a RAF loaded forgets
     * it the moment the cable moves, so the object that remembers has to die with the session
     * it belongs to.
     */
    private var labSession: RawLabSession? = null

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

    private suspend fun connectNow(device: UsbDevice?) {
        lock.withLock {
            if (_state.value is CameraState.Connected || _state.value is CameraState.Writing) return@withLock

            if (device == null) {
                _state.value = CameraState.Error(
                    "No camera is attached. Connect it with a USB-C cable, and set the camera's " +
                        "USB mode to the RAW-conversion/backup mode.",
                )
                return@withLock
            }

            _state.value = CameraState.Connecting

            if (!usbManager.hasPermission(device)) {
                requestPermission(device)
                // The broadcast receiver picks it up from here, in either direction.
                return@withLock
            }

            openSession(device)
        }
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
            // A fresh body is holding no RAF, so the lab starts again from an empty camera.
            labSession = RawLabSession()
            // Both reads swallow their own failures and answer "not reported", so a body that
            // will not discuss its USB mode or its battery still connects normally.
            _state.value = CameraState.Connected(
                identity = CameraModels.identify(info.model),
                usbMode = readUsbMode(opened, info),
                details = readCameraDetails(opened, info),
            )

            // A connected camera is worth a foreground service on its own: it keeps the USB
            // session alive while the app is minimised, and it ties the notification to the
            // process that actually holds the connection, so a killed process cannot leave a
            // notification behind pointing at a camera nobody is talking to.
            CameraTransferService.start(appContext)
        } catch (error: Exception) {
            session = null
            labSession = null
            _state.value = error.toCameraError()
        }
    }

    /**
     * Called when a camera USB detachment is detected.
     */
    fun onDeviceDetached(device: UsbDevice?) {
        if (!hasHostSupport) return
        scope.launch {
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

    // ─── Disconnecting ──────────────────────────────────────────────────────

    fun disconnect() {
        scope.launch { lock.withLock { closeSession(CameraState.Disconnected) } }
    }

    /**
     * Reopens the attached body so facts that belong to its current USB mode are read again.
     *
     * Fuji bodies may re-enumerate when the user changes CONNECTION MODE, but not every body or
     * Android host reports that transition consistently. The Photos screen therefore offers an
     * explicit refresh while connected in another mode.
     */
    fun reconnect() {
        if (!hasHostSupport) return
        scope.launch {
            lock.withLock { closeSession(CameraState.Disconnected) }
            connectNow(UsbBulkChannel.findCamera(usbManager))
        }
    }

    private fun closeSession(next: CameraState) {
        session?.let { runCatching { it.close() } }
        session = null
        labSession = null
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
            ContextCompat.RECEIVER_EXPORTED,
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
                    val attachedDevice = IntentCompat.getParcelableExtra(
                        intent,
                        UsbManager.EXTRA_DEVICE,
                        UsbDevice::class.java,
                    )
                    if (attachedDevice != null && attachedDevice.vendorId != FUJI_VENDOR_ID) {
                        return
                    }
                    if (_state.value is CameraState.Disconnected) {
                        onDeviceAttached(attachedDevice)
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val detachedDevice = IntentCompat.getParcelableExtra(
                        intent,
                        UsbManager.EXTRA_DEVICE,
                        UsbDevice::class.java,
                    )
                    if (detachedDevice != null && detachedDevice.vendorId != FUJI_VENDOR_ID) {
                        return
                    }
                    onDeviceDetached(detachedDevice)
                }
            }
        }
    }

    // ─── Reading the slots (FEAT-006) ───────────────────────────────────────

    /**
     * Asks the camera what each of C1–C7 currently holds.
     *
     * Behind the same lock as everything else, so it cannot run alongside a write — they share
     * the slot selector, and interleaving them would leave the selector pointing somewhere the
     * write did not choose.
     *
     * A per-slot refusal is that slot's own answer and comes back as unread. A failure of the
     * pipe itself throws, and the sheet says the camera did not answer rather than showing
     * seven slots it never asked about.
     */
    suspend fun readSlots(): List<SlotNameReading> = lock.withLock {
        val open = session ?: return emptyList()

        return withContext(Dispatchers.IO) { readSlotNames(open) }
    }

    /**
     * Reads every slot as a recipe — FEAT-007's import.
     *
     * Behind the same lock and for the same reason as [readSlots]: it drives the slot selector,
     * and interleaving it with a write would leave the selector pointing somewhere the write
     * did not choose.
     *
     * Slower than [readSlots] by roughly the number of properties per slot, so the progress
     * callback is not decoration — this is seconds of work with a cable in.
     */
    suspend fun readSlotRecipes(
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): List<SlotRecipe> = lock.withLock {
        val open = session ?: return emptyList()

        return withContext(Dispatchers.IO) { readSlotRecipes(open, onProgress) }
    }

    /**
     * Reads a single slot as a recipe.
     */
    suspend fun readSlotRecipe(slot: Int): SlotRecipe? = lock.withLock {
        val open = session ?: return null

        return withContext(Dispatchers.IO) { readSlotRecipe(open, slot) }
    }

    // ─── Reading photos from the card ──────────────────────────────────────

    suspend fun listCameraPhotos(
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): List<CameraMediaObject> = lock.withLock {
        val open = mediaSession()
        withContext(Dispatchers.IO) { listCameraJpegs(open, onProgress) }
    }

    suspend fun listCameraFiles(
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): List<CameraMediaObject> = lock.withLock {
        val open = mediaSession()
        withContext(Dispatchers.IO) { listCameraFiles(open, onProgress) }
    }

    suspend fun downloadCameraJpeg(
        media: CameraMediaObject,
        output: OutputStream,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
    ): Long = lock.withLock {
        val open = mediaSession()
        withContext(Dispatchers.IO) {
            downloadCameraJpeg(
                open,
                media,
                output,
                onProgress = onProgress,
                isCancelled = isCancelled,
            )
        }
    }

    suspend fun downloadCameraRaf(
        media: CameraMediaObject,
        output: OutputStream,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
        isCancelled: () -> Boolean = { false },
    ): Long = lock.withLock {
        val open = mediaSession()
        withContext(Dispatchers.IO) {
            downloadCameraRaf(
                open,
                media,
                output,
                onProgress = onProgress,
                isCancelled = isCancelled,
            )
        }
    }

    suspend fun readCameraPhotoThumbnail(handle: Int): ByteArray? = lock.withLock {
        val open = mediaSession()
        withContext(Dispatchers.IO) { readCameraThumbnail(open, handle) }
    }

    suspend fun downloadCameraPhoto(
        media: CameraMediaObject,
        cache: CameraMediaCache,
        onProgress: (written: Long, total: Long) -> Unit = { _, _ -> },
    ): File = lock.withLock {
        val open = mediaSession()
        withContext(Dispatchers.IO) { cache.download(open, media, onProgress) }
    }

    private fun mediaSession(): PtpSession {
        val open = session ?: throw IllegalStateException(
            "Connect the camera before choosing photos from it.",
        )
        val connected = _state.value as? CameraState.Connected ?: throw IllegalStateException(
            "The camera is busy. Wait for the current operation to finish.",
        )
        // Positively-identified non-card-reader modes only. `UNREPORTED` passes: it means the
        // body would not say, and refusing on "we could not tell" would block a camera that is
        // set correctly. `CARD_READER` is now positively identified and passes on its own
        // merits rather than by falling through.
        if (connected.usbMode != UsbMode.CARD_READER &&
            connected.usbMode != UsbMode.UNREPORTED
        ) {
            throw IllegalStateException(
                "Photo browsing requires USB CARD READER mode. Change the camera's USB mode, " +
                    "then disconnect and reconnect the cable.",
            )
        }
        return open
    }

    // ─── In-camera RAW development ─────────────────────────────────────────

    /**
     * Renders [settings] against [raf] on the camera, uploading the file only when needed.
     *
     * The lock is held for one render, not for the lab session: someone who leaves the lab open
     * while checking the camera's photo list should not find the connection wedged. What
     * survives between renders is [labSession]'s knowledge of which RAF the camera holds, and
     * that is tied to the PTP session rather than to this call.
     */
    suspend fun renderRawInLab(
        raf: File,
        settings: JsonObject,
        output: File,
        quality: RawRenderQuality = RawRenderQuality.FULL,
        onStage: (RawDevelopmentStage) -> Unit = {},
    ): RawDevelopmentResult = lock.withLock {
        val open = rawDevelopmentSession()
        val lab = labSession ?: RawLabSession().also { labSession = it }
        underRawWakeLock {
            withContext(Dispatchers.IO) {
                lab.render(open, raf, settings, output, quality, onStage)
            }
        }
    }

    /**
     * The camera's own `0xD185` block for [raf] — the fixture an uncalibrated body needs.
     *
     * Goes through the same [labSession], so capturing a profile and then rendering does not
     * upload the file twice.
     */
    suspend fun captureRawDevelopmentProfile(
        raf: File,
        onStage: (RawDevelopmentStage) -> Unit = {},
    ): ByteArray = lock.withLock {
        val open = rawDevelopmentSession()
        val lab = labSession ?: RawLabSession().also { labSession = it }
        underRawWakeLock { withContext(Dispatchers.IO) { lab.profile(open, raf, onStage) } }
    }

    /**
     * Runs camera work that must not be interrupted by the phone sleeping.
     *
     * A framing or timeout error means the session is no longer trustworthy, so it is closed
     * here rather than left for the next caller to trip over — which also drops [labSession],
     * and with it the belief that the camera is still holding a RAF.
     */
    private suspend fun <T> underRawWakeLock(block: suspend () -> T): T {
        val wakeLock = (appContext.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
        try {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
            return block()
        } catch (error: Exception) {
            if (error is PtpFramingError || error is PtpTimeoutError) {
                closeSession(error.toCameraError())
            }
            throw error
        } finally {
            if (wakeLock.isHeld) wakeLock.release()
        }
    }

    private fun rawDevelopmentSession(): PtpSession {
        val open = session ?: throw IllegalStateException(
            "Connect the camera before developing a RAW file.",
        )
        val connected = _state.value as? CameraState.Connected ?: throw IllegalStateException(
            "The camera is busy. Wait for the current operation to finish.",
        )
        if (connected.usbMode != UsbMode.RAW_CONVERSION &&
            connected.usbMode != UsbMode.UNREPORTED
        ) {
            throw IllegalStateException(
                "RAW development requires USB RAW CONV./BACKUP RESTORE mode. Change the camera " +
                    "setting, then disconnect and reconnect the cable.",
            )
        }
        return open
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

    // ─── Diagnostics ────────────────────────────────────────────────────────

    /**
     * Walks every property the body will discuss and returns the report.
     *
     * Behind the same lock as everything else: it drives the slot selector to C1, so running it
     * alongside a write would leave the selector somewhere the write did not choose — the same
     * reason [readSlots] is serialised.
     */
    suspend fun readReport(
        appVersion: String,
        capturedAt: String,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): CameraReport = lock.withLock {
        val open = session ?: throw IllegalStateException(
            "The camera is not connected, so there is nothing to report on.",
        )

        return withContext(Dispatchers.IO) {
            readCameraReport(open, appVersion, capturedAt, onProgress)
        }
    }

    /** The open session, or null. */
    internal fun openSessionOrNull(): PtpSession? = session

    companion object {
        /** Intent action used for USB permission callbacks. */
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

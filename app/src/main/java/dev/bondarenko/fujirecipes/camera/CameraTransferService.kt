package dev.bondarenko.fujirecipes.camera

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dev.bondarenko.fujirecipes.FujiRecipesApp
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The thing that keeps a connected camera, and a download, alive.
 *
 * A wake lock alone does not: it keeps the CPU awake, but it gives Android no reason to spare
 * the process. Once the activity stops, a process holding nothing but a coroutine is cached,
 * and a cached process may be killed at any moment — a risk that grows with exactly the thing
 * this app does, a transfer measured in minutes. A `connectedDevice` foreground service is the
 * mechanism Android provides for a user-initiated transfer over an attached USB device.
 *
 * It runs for as long as a camera is connected, not only during a batch, which does two things:
 * the USB session survives minimising the app, and the notification is tied to the process that
 * actually holds the connection — so if the process dies, the notification goes with it rather
 * than sitting in the shade pointing at a camera nobody is talking to.
 *
 * The notification is presentation, not protection. If the user denied notifications, or the
 * manufacturer refuses to promote a transfer to a Live Update, everything still runs.
 */
class CameraTransferService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var batchJob: Job? = null
    private var watcher: Job? = null
    private var lastPostedAt = 0L
    private var lastNotification: Notification? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = (application as FujiRecipesApp).container
        val transfers = container.cameraTransfers

        if (intent?.action == ACTION_CANCEL) {
            transfers.cancel()
            return START_NOT_STICKY
        }

        // Foreground within five seconds of `startForegroundService`, whatever the reason for
        // the start. Rendered from state read synchronously so there is nothing to wait for.
        ensureTransferChannel(this)
        val cameraState = container.cameraController.state.value
        val batchInFlight = transfers.state.value is CameraTransferState.Running
        if (cameraState !is CameraState.Connected && !batchInFlight) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        promote(render(cameraState, transfers.state.value))

        startWatching()
        startPendingBatch()

        return START_NOT_STICKY
    }

    /**
     * One collector for both sources, because the notification is a function of both: a batch
     * in flight outranks the connection behind it, and when the batch ends the connection is
     * what is left to show.
     *
     * It is also what stops the service. A camera that is no longer connected and has no batch
     * running has nothing left to keep the process alive for.
     */
    private fun startWatching() {
        if (watcher?.isActive == true) return

        val container = (application as FujiRecipesApp).container
        watcher = scope.launch {
            combine(
                container.cameraController.state,
                container.cameraTransfers.state,
            ) { camera, transfer -> camera to transfer }
                .collect { (camera, transfer) ->
                    val notification = render(camera, transfer)
                    if (notification == null) {
                        if (batchJob?.isActive != true) finish()
                        return@collect
                    }
                    post(notification, throttled = transfer is CameraTransferState.Running)
                }
        }
    }

    /**
     * A batch in flight wins; a connected camera is the resting state; anything else is nothing
     * to show, and null is the signal to stop.
     */
    private fun render(camera: CameraState, transfer: CameraTransferState?): Notification? {
        val label = (camera as? CameraState.Connected)?.identity?.model
            ?: getString(R.string.camera_transfer_unknown_camera)

        return when {
            transfer is CameraTransferState.Running ->
                buildTransferNotification(this, label, transfer.progress)

            camera is CameraState.Connected ->
                buildConnectedNotification(this, label, camera.usbMode)

            // Writing a slot is the controller's own foreground concern and has its own wake
            // lock; this service has nothing to add to it, but it must not stop mid-write
            // either, so it holds whatever it was already showing.
            camera is CameraState.Writing -> lastNotification

            else -> null
        }
    }

    private fun startPendingBatch() {
        if (batchJob?.isActive == true) return
        val transfers = (application as FujiRecipesApp).container.cameraTransfers
        val batch = transfers.takePending() ?: return

        batchJob = scope.launch {
            val wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)

            try {
                wakeLock.acquire(BATCH_WAKE_LOCK_MS)
                transfers.run(batch)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                // Not `finish()`: the camera is usually still connected, and the watcher will
                // fall back to the connected notification. It stops the service if it is not.
            }
        }
    }

    /**
     * `START_NOT_STICKY` above and nothing to restore here: the USB connection did not survive
     * whatever killed us, so a service restarted by the system would have no camera to talk to.
     * Recovery is the on-disk record and the notice the app shows on next launch.
     */
    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun promote(notification: Notification?) {
        val shown = notification ?: buildConnectedNotification(
            this,
            getString(R.string.camera_transfer_unknown_camera),
            UsbMode.UNREPORTED,
        )
        lastNotification = shown
        ServiceCompat.startForeground(
            this,
            TRANSFER_NOTIFICATION_ID,
            shown,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )
    }

    /**
     * Transfer progress arrives once per 64 KiB chunk — hundreds of times for one image.
     * Posting each would be throttled by the system anyway and would cost more than it shows,
     * so those are refreshed about once a second. A connection change is not throttled: there
     * are a handful of them and each one changes what the actions mean.
     */
    private fun post(notification: Notification, throttled: Boolean) {
        if (throttled) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastPostedAt < NOTIFICATION_INTERVAL_MS) return
            lastPostedAt = now
        }

        lastNotification = notification
        val manager = getSystemService<NotificationManager>() ?: return
        runCatching { manager.notify(TRANSFER_NOTIFICATION_ID, notification) }
    }

    companion object {
        const val ACTION_CANCEL = "dev.bondarenko.fujirecipes.CANCEL_CAMERA_TRANSFER"

        private const val WAKE_LOCK_TAG = "FujiRecipes:cameraTransfer"

        /**
         * A ceiling, not an expectation. A full card of large RAFs is the long case; the lock
         * is released the moment the batch ends either way, and a timeout means a stuck
         * transfer cannot hold the CPU awake indefinitely.
         *
         * Held only for a batch. A camera sitting connected does not keep the CPU awake.
         */
        private const val BATCH_WAKE_LOCK_MS = 60L * 60L * 1000L

        private const val NOTIFICATION_INTERVAL_MS = 1_000L

        /**
         * Starting a foreground service is refused outright when the app is not visible
         * (Android 12+). Every caller here runs from a foreground interaction or from the
         * attach intent that launched the activity, so the refusal should not arise — and if
         * it does, it costs the notification, never the connection or the transfer.
         */
        fun start(context: Context) {
            val intent = Intent(context, CameraTransferService::class.java)
            runCatching { ContextCompat.startForegroundService(context, intent) }
        }

        /**
         * Deliberately `startService`, not `startForegroundService`: a service started the
         * latter way owes Android a `startForeground` within five seconds, and a cancel has no
         * batch to show. Cancel only ever reaches a service that is already running.
         */
        fun cancel(context: Context) {
            val intent = Intent(context, CameraTransferService::class.java)
                .setAction(ACTION_CANCEL)
            runCatching { context.startService(intent) }
        }
    }
}

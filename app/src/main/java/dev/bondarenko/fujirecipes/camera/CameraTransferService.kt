package dev.bondarenko.fujirecipes.camera

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
import dev.bondarenko.fujirecipes.core.store.CameraExportProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The thing that keeps a camera download alive.
 *
 * A wake lock alone does not: it keeps the CPU awake, but it gives Android no reason to spare
 * the process. Once the activity stops, a process holding nothing but a coroutine is cached,
 * and a cached process may be killed at any moment — a risk that grows with exactly the thing
 * this app does, a transfer measured in minutes. A `connectedDevice` foreground service is the
 * mechanism Android provides for a user-initiated transfer over an attached USB device, and it
 * is what makes "minimise the app" a supported thing to do rather than a gamble.
 *
 * The notification is presentation, not protection. If the user denied notifications, or the
 * manufacturer refuses to promote it to a Live Update, the batch still runs to the end.
 */
class CameraTransferService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var batchJob: Job? = null
    private var lastPostedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val transfers = (application as FujiRecipesApp).container.cameraTransfers

        if (intent?.action == ACTION_CANCEL) {
            transfers.cancel()
            if (batchJob?.isActive != true) stopSelf(startId)
            return START_NOT_STICKY
        }

        // A second start while a batch is running would run two transfers down one USB pipe.
        val batch = if (batchJob?.isActive == true) null else transfers.takePending()
        if (batch == null) {
            if (batchJob?.isActive != true) stopSelf(startId)
            return START_NOT_STICKY
        }

        ensureTransferChannel(this)
        val initial = transfers.state.value as? CameraTransferState.Running
        ServiceCompat.startForeground(
            this,
            TRANSFER_NOTIFICATION_ID,
            buildTransferNotification(
                context = this,
                cameraLabel = batch.cameraLabel,
                progress = initial?.progress ?: startingProgress(batch),
            ),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
        )

        batchJob = scope.launch {
            val wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)

            val notifier = launch {
                transfers.state.collect { state ->
                    if (state is CameraTransferState.Running) {
                        post(batch.cameraLabel, state)
                    }
                }
            }

            try {
                wakeLock.acquire(BATCH_WAKE_LOCK_MS)
                transfers.run(batch)
            } finally {
                notifier.cancel()
                if (wakeLock.isHeld) wakeLock.release()
                ServiceCompat.stopForeground(
                    this@CameraTransferService,
                    ServiceCompat.STOP_FOREGROUND_REMOVE,
                )
                stopSelf()
            }
        }

        return START_NOT_STICKY
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

    /**
     * Progress arrives once per 64 KiB chunk — hundreds of times for one image. Posting each
     * one would be throttled by the system anyway and would cost more than it shows, so the
     * notification is refreshed about once a second.
     */
    private fun post(cameraLabel: String, state: CameraTransferState.Running) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPostedAt < NOTIFICATION_INTERVAL_MS) return
        lastPostedAt = now

        val manager = getSystemService<NotificationManager>() ?: return
        runCatching {
            manager.notify(
                TRANSFER_NOTIFICATION_ID,
                buildTransferNotification(this, cameraLabel, state.progress),
            )
        }
    }

    private fun startingProgress(batch: CameraTransferJob) =
        CameraExportProgress(
            currentFile = 1,
            totalFiles = batch.files.size,
            filename = batch.files.first().info.filename,
            written = 0,
            totalBytes = batch.files.sumOf { it.info.compressedSize },
        )

    companion object {
        const val ACTION_CANCEL = "dev.bondarenko.fujirecipes.CANCEL_CAMERA_TRANSFER"

        private const val WAKE_LOCK_TAG = "FujiRecipes:cameraTransfer"

        /**
         * A ceiling, not an expectation. A full card of large RAFs is the long case; the lock
         * is released the moment the batch ends either way, and a timeout means a stuck
         * transfer cannot hold the CPU awake indefinitely.
         */
        private const val BATCH_WAKE_LOCK_MS = 60L * 60L * 1000L

        private const val NOTIFICATION_INTERVAL_MS = 1_000L

        fun start(context: Context) {
            val intent = Intent(context, CameraTransferService::class.java)
            ContextCompat.startForegroundService(context, intent)
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

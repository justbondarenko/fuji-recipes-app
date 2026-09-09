package dev.bondarenko.fujirecipes.camera

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import dev.bondarenko.fujirecipes.MainActivity
import dev.bondarenko.fujirecipes.R
import dev.bondarenko.fujirecipes.core.store.CameraExportProgress

const val TRANSFER_NOTIFICATION_ID = 4101

private const val TRANSFER_CHANNEL_ID = "camera-transfers"

/**
 * The channel is `IMPORTANCE_LOW` on purpose: a transfer the user started and is watching does
 * not need to make a sound every time it moves.
 */
fun ensureTransferChannel(context: Context) {
    val manager = context.getSystemService<NotificationManager>() ?: return
    if (manager.getNotificationChannel(TRANSFER_CHANNEL_ID) != null) return

    manager.createNotificationChannel(
        NotificationChannel(
            TRANSFER_CHANNEL_ID,
            context.getString(R.string.camera_transfer_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.camera_transfer_channel_description)
            setShowBadge(false)
        },
    )
}

/**
 * The ongoing notification for one batch.
 *
 * On Android 16 and above this asks to be promoted to a Live Update, which is what puts the
 * progress on the lock screen and in the status bar chip. Promotion is a request, not a
 * guarantee — the user or the manufacturer can refuse it — so everything the notification says
 * has to make sense as an ordinary progress notification too, and the transfer's correctness
 * rests on the foreground service rather than on any of this being visible.
 */
fun buildTransferNotification(
    context: Context,
    cameraLabel: String,
    progress: CameraExportProgress,
): Notification {
    val percent = progress.percent()
    val builder = NotificationCompat.Builder(context, TRANSFER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_transfer)
        .setContentTitle(
            context.getString(
                R.string.camera_transfer_notification_title,
                progress.currentFile,
                progress.totalFiles,
            ),
        )
        .setContentText(progress.filename)
        .setSubText(cameraLabel)
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(openAppIntent(context))
        .addAction(
            R.drawable.ic_notification_transfer,
            context.getString(R.string.camera_transfer_cancel),
            cancelIntent(context),
        )

    if (Build.VERSION.SDK_INT >= LIVE_UPDATE_SDK) {
        builder
            .setStyle(NotificationCompat.ProgressStyle().setProgress(percent))
            .setShortCriticalText("$percent%")
            .setRequestPromotedOngoing(true)
    } else {
        builder.setProgress(100, percent, false)
    }

    return builder.build()
}

/** Android 16, where Live Updates arrived. Named rather than inlined so the reason is visible. */
private const val LIVE_UPDATE_SDK = 36

private fun CameraExportProgress.percent(): Int =
    if (totalBytes <= 0L) 0 else ((written * 100) / totalBytes).toInt().coerceIn(0, 100)

private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
    context,
    0,
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    },
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

private fun cancelIntent(context: Context): PendingIntent = PendingIntent.getService(
    context,
    1,
    Intent(context, CameraTransferService::class.java).setAction(
        CameraTransferService.ACTION_CANCEL,
    ),
    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
)

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
import dev.bondarenko.fujirecipes.camera.plan.UsbMode
import dev.bondarenko.fujirecipes.core.store.CameraExportProgress

const val TRANSFER_NOTIFICATION_ID = 4101

/** Which screen a notification action should land on. Read by `MainActivity`. */
const val EXTRA_DESTINATION = "dev.bondarenko.fujirecipes.extra.DESTINATION"

const val DESTINATION_PHOTOS = "camera-photos"
const val DESTINATION_CAMERA = "camera"

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

/**
 * The notification for a camera that is connected and doing nothing.
 *
 * **Promoted, like the transfer.** A connected camera is a state rather than a process with a
 * start and a finish, so this is not what Live Updates were designed around — but a plugged-in
 * camera with the app in the background is exactly when the status-bar chip earns its place,
 * and that was the call made after seeing both on a device.
 *
 * Promotion needs no `ProgressStyle`: a standard-style notification qualifies as long as it is
 * ongoing, carries a content title, uses no custom views, is not a group summary, is **not**
 * colorized, and its channel is not `IMPORTANCE_MIN`. All of those hold here — the colorized
 * one is the trap, since a foreground-service notification is the usual place to reach for it.
 * [setShortCriticalText] is what the chip itself shows, so it stays to a word or two.
 *
 * It is still only a request. The user or the manufacturer can refuse it, so everything below
 * has to read correctly as an ordinary ongoing notification too.
 *
 * Its action is chosen by the USB mode, because the two modes lead to different screens and
 * offering the wrong one is worse than offering none: browsing the card needs
 * `USB CARD READER`, and everything to do with recipes needs `USB RAW CONV./BACKUP RESTORE`.
 * A mode this build cannot name gets no action at all — tapping the notification still opens
 * the app, which is the honest fallback when we do not know what the camera can do.
 */
fun buildConnectedNotification(
    context: Context,
    cameraLabel: String,
    mode: UsbMode,
): Notification {
    val builder = NotificationCompat.Builder(context, TRANSFER_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification_transfer)
        .setContentTitle(cameraLabel)
        .setContentText(context.getString(modeDescription(mode)))
        .setOngoing(true)
        .setSilent(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setContentIntent(openAppIntent(context))

    if (Build.VERSION.SDK_INT >= LIVE_UPDATE_SDK) {
        builder
            .setShortCriticalText(context.getString(modeShortLabel(mode)))
            .setRequestPromotedOngoing(true)
    }

    when {
        mode.allowsCardBrowsing -> builder.addAction(
            R.drawable.ic_notification_transfer,
            context.getString(R.string.camera_connected_action_photos),
            openDestinationIntent(context, DESTINATION_PHOTOS),
        )

        mode.allowsRecipeWork -> builder.addAction(
            R.drawable.ic_notification_transfer,
            context.getString(R.string.camera_connected_action_camera),
            openDestinationIntent(context, DESTINATION_CAMERA),
        )
    }

    return builder.build()
}

/** What the camera can be used for right now, in the user's terms rather than the protocol's. */
private fun modeDescription(mode: UsbMode): Int = when (mode) {
    UsbMode.CARD_READER -> R.string.camera_connected_card_reader
    UsbMode.RAW_CONVERSION -> R.string.camera_connected_raw_conversion
    UsbMode.TETHER_SHOOTING -> R.string.camera_connected_tether
    UsbMode.WEBCAM -> R.string.camera_connected_webcam
    UsbMode.UNRECOGNISED, UsbMode.UNREPORTED -> R.string.camera_connected_unknown_mode
}

/** A word or two, for the status-bar chip. Anything longer is truncated by the system. */
private fun modeShortLabel(mode: UsbMode): Int = when (mode) {
    UsbMode.CARD_READER -> R.string.camera_connected_short_card_reader
    UsbMode.RAW_CONVERSION -> R.string.camera_connected_short_raw_conversion
    UsbMode.TETHER_SHOOTING -> R.string.camera_connected_short_tether
    UsbMode.WEBCAM -> R.string.camera_connected_short_webcam
    UsbMode.UNRECOGNISED, UsbMode.UNREPORTED -> R.string.camera_connected_short_unknown_mode
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

/**
 * Opens the app on a named screen.
 *
 * The request code is derived from the destination: two `PendingIntent`s that differ only in
 * their extras and share a request code are the *same* intent as far as the system is
 * concerned, and the second would silently reuse the first's extras.
 */
private fun openDestinationIntent(context: Context, destination: String): PendingIntent =
    PendingIntent.getActivity(
        context,
        destination.hashCode(),
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, destination)
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

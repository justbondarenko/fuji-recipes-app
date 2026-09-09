package dev.bondarenko.fujirecipes.ui.cameraphotos

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private val PTP_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

/** Converts the compact PTP timestamp into the phone locale's date and time format. */
internal fun formatCameraCaptureDate(
    value: String,
    locale: Locale = Locale.getDefault(),
): String? = runCatching {
    LocalDateTime.parse(value.take(15), PTP_DATE_TIME).format(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale),
    )
}.getOrNull()

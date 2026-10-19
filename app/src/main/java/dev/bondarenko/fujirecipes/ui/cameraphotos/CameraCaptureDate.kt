package dev.bondarenko.fujirecipes.ui.cameraphotos

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.ResolverStyle
import java.util.Locale

private val PTP_DATE_TIME = DateTimeFormatter.ofPattern("uuuuMMdd'T'HHmmss")
    .withResolverStyle(ResolverStyle.STRICT)

private fun parseCameraCaptureDate(value: String): LocalDateTime? = runCatching {
    LocalDateTime.parse(value.take(15), PTP_DATE_TIME)
}.getOrNull()

/** Camera-local date: timezone suffixes must not move a photo into a different day. */
internal fun cameraCaptureDay(value: String): LocalDate? = parseCameraCaptureDate(value)?.toLocalDate()

internal fun formatCameraCaptureDay(date: LocalDate, locale: Locale = Locale.getDefault()): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))

/** Converts the compact PTP timestamp into the phone locale's date and time format. */
internal fun formatCameraCaptureDate(
    value: String,
    locale: Locale = Locale.getDefault(),
): String? = parseCameraCaptureDate(value)?.format(
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale),
)

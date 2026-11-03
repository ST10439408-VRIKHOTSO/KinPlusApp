package za.co.kinplus.app.ui.screens.journey

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/*
 * The API returns a journey's ETA as a UTC ISO 8601 string, for example
 * 2026-11-03T15:15:17.6280000Z. The journey card shows it as a short local
 * time instead, and adds the date only when the ETA is not today.
 */

/** Parses an ISO 8601 ETA, or null if it can't be read. A value with no offset is taken as UTC. */
internal fun parseEta(iso: String): Instant? =
    runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
        ?: runCatching { LocalDateTime.parse(iso).toInstant(ZoneOffset.UTC) }.getOrNull()

/** Short local time for [iso] (e.g. "17:15", or "4 Nov, 01:30" on another day); unreadable values are shown as they are. */
internal fun formatEta(
    iso: String,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
    now: Instant = Instant.now()
): String {
    val eta = parseEta(iso)?.atZone(zone) ?: return iso
    val time = eta.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    if (eta.toLocalDate() == now.atZone(zone).toLocalDate()) return time
    return "${eta.format(DateTimeFormatter.ofPattern("d MMM", locale))}, $time"
}

package elemsocial.com.core.time

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

private data class WordForms(
    val singular: String,
    val few: String,
    val many: String
)

private val minuteForms = WordForms("минуту", "минуты", "минут")
private val hourForms = WordForms("час", "часа", "часов")
private val dayForms = WordForms("день", "дня", "дней")
private val weekForms = WordForms("неделю", "недели", "недель")
private val monthForms = WordForms("месяц", "месяца", "месяцев")
private val yearForms = WordForms("год", "года", "лет")

private val localFormatters = listOf(
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS", Locale.ROOT),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT),
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)
)

fun formatTimeAge(
    inputDate: String?,
    showDetailed: Boolean = false,
    now: ZonedDateTime = ZonedDateTime.now()
): String {
    val input = parseInputDate(inputDate, now.zone) ?: return "сейчас"
    if (now.isBefore(input)) return "сейчас"

    val seconds = ChronoUnit.SECONDS.between(input, now)
    val minutes = ChronoUnit.MINUTES.between(input, now)
    val hours = ChronoUnit.HOURS.between(input, now)
    val days = ChronoUnit.DAYS.between(input, now)
    val weeks = ChronoUnit.WEEKS.between(input, now)
    val months = ChronoUnit.MONTHS.between(input, now)
    val years = ChronoUnit.YEARS.between(input, now)

    if (seconds < 60) return "сейчас"
    if (minutes < 60) return "$minutes ${declension(minutes, minuteForms)} назад"
    if (hours < 24) return "$hours ${declension(hours, hourForms)} назад"

    if (!showDetailed) {
        if (days < 7) return "$days ${declension(days, dayForms)} назад"
        if (days < 31 && months == 0L) return "$weeks ${declension(weeks, weekForms)} назад"
        if (months < 12) {
            if (months == 0L) return "$days ${declension(days, dayForms)} назад"
            return "$months ${declension(months, monthForms)} назад"
        }
        return "$years ${declension(years, yearForms)} назад"
    }

    val remainingMonths = months - years * 12

    if (years > 0 && remainingMonths > 0) {
        return "$years ${declension(years, yearForms)} и $remainingMonths ${declension(remainingMonths, monthForms)} назад"
    }

    if (years > 0) {
        return "$years ${declension(years, yearForms)} назад"
    }

    if (months > 0) {
        return "$months ${declension(months, monthForms)} назад"
    }

    return "$days ${declension(days, dayForms)} назад"
}

private fun parseInputDate(raw: String?, zoneId: ZoneId): ZonedDateTime? {
    val value = raw?.trim().orEmpty()
    if (value.isEmpty()) return null

    runCatching {
        return Instant.parse(value).atZone(zoneId)
    }

    runCatching {
        return ZonedDateTime.parse(value)
    }

    runCatching {
        return OffsetDateTime.parse(value).atZoneSameInstant(zoneId)
    }

    runCatching {
        return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(zoneId)
    }

    runCatching {
        return LocalDateTime.parse(value.replace(' ', 'T'), DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(zoneId)
    }

    for (formatter in localFormatters) {
        try {
            return LocalDateTime.parse(value, formatter).atZone(zoneId)
        } catch (_: DateTimeParseException) {
            // no-op
        }
    }

    return null
}

private fun declension(value: Long, forms: WordForms): String {
    val n = abs(value % 100)
    val n1 = n % 10
    return when {
        n in 11..19 -> forms.many
        n1 == 1L -> forms.singular
        n1 in 2L..4L -> forms.few
        else -> forms.many
    }
}

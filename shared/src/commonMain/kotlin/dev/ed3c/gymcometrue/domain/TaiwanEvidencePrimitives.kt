package dev.ed3c.gymcometrue.domain

/**
 * Shared fail-closed primitives for the Taiwan corpus, OCR-evaluation, and reviewed rule-pack
 * contracts.
 *
 * The two older Taiwan files each carry their own file-private copies of these checks. These
 * declarations use deliberately distinct names so that adding a shared helper cannot collide with
 * an existing file-private declaration in the same package.
 */

private val taiwanSha256Pattern = Regex("^[0-9a-f]{64}$")

/** A hash is only usable evidence when it is a lowercase SHA-256. A null hash is never evidence. */
internal fun String?.isTaiwanSha256(): Boolean = this != null && taiwanSha256Pattern.matches(this)

/**
 * Returns a comparable `YYYYMMDD` key, or null when the value is absent or is not a real calendar
 * date. Callers must treat null as `ABSENT`, never as "today" and never as "valid".
 */
internal fun String?.taiwanIsoDateKey(): Int? {
    if (this == null) return null
    val parts = split("-")
    if (parts.size != 3 || parts[0].length != 4 || parts[1].length != 2 || parts[2].length != 2) return null
    val year = parts[0].toIntOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12) return null
    val maxDay = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)) 29 else 28
        else -> return null
    }
    if (day !in 1..maxDay) return null
    return year * 10_000 + month * 100 + day
}

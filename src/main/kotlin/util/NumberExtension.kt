package util

/**
 * Example:
 * ```kt
 * 5.secondsToTicks() // -> 100 ticks
 * ```
 */
@Suppress("UNUSED")
fun Int.secondsToTicks(): Int {
    return this.times(20)
}

fun Int.timeRemainingFormatted(): String {
    if (this < 0) return "Invalid time supplied"

    val hours = this / 60
    val mins = this % 60

    return when {
        hours > 0 && mins > 0 -> "${hours}hr ${mins}m"
        hours > 0 -> "${hours}hr"
        mins > 0 -> "${mins}m"
        else -> "0m"
    }
}

fun Long.timeRemainingFormatted(): String {
    return this.toInt().timeRemainingFormatted()
}

fun Int.timeRemainingFormattedSeconds(): String {
    if (this < 0) return "Invalid time supplied"

    val hours = this / 3600
    val mins = (this % 3600) / 60
    val secs = this % 60

    return when {
        hours > 0 && mins > 0 && secs > 0 -> "${hours}hr ${mins}m ${secs}s"
        hours > 0 && mins > 0 -> "${hours}hr ${mins}m"
        hours > 0 && secs > 0 -> "${hours}hr ${secs}s"
        hours > 0 -> "${hours}hr"
        mins > 0 && secs > 0 -> "${mins}m ${secs}s"
        mins > 0 -> "${mins}m"
        secs > 0 -> "${secs}s"
        else -> "0s"
    }
}

fun Long.timeRemainingFormattedSeconds(): String {
    return this.toInt().timeRemainingFormattedSeconds()
}
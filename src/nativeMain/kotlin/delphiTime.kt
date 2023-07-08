import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

val delphiToUnixOffset: Long = (-25569).days.inWholeSeconds

fun delphiLocalEpochDaysToUnixUtcEpochSeconds(daysSinceDelphiEpoch: Double): Long {
    val secondsSinceLocalUnixEpoch = daysSinceDelphiEpoch.days.inWholeSeconds + delphiToUnixOffset
    return Instant.fromEpochSeconds(secondsSinceLocalUnixEpoch)
        .toLocalDateTime(TimeZone.UTC)
        .toInstant(TimeZone.currentSystemDefault())
        .epochSeconds
}
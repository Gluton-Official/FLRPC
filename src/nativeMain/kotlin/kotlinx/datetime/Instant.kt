package kotlinx.datetime

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

val Instant.Companion.ZERO: Instant get() = fromEpochSeconds(0)

fun Duration.toInstant(): Instant = Instant.ZERO + this
fun Instant.toDuration(): Duration = epochSeconds.seconds + nanosecondsOfSecond.nanoseconds
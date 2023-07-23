package dev.gluton.flrpc.delphi

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

value class DelphiInstant private constructor(val days: Double) {

    fun toUnixInstant(): Instant = delphiToUnix(days).toDuration(DurationUnit.DAYS).toInstant()

    companion object {
        private inline fun delphiToUnix(days: Double): Double = days - 25569
        private inline fun unixToDelphi(days: Double): Double = days + 25569

        /**
         * Creates a UTC [DelphiInstant] from days since the Delphi epoch
         */
        fun fromEpochDays(days: Double, sourceTimeZone: TimeZone = TimeZone.UTC): DelphiInstant {
            val delphiInstant = DelphiInstant(days)
            return when {
                sourceTimeZone != TimeZone.UTC -> delphiInstant
                    .toUnixInstant()
                    .toLocalDateTime(TimeZone.UTC)
                    .toInstant(sourceTimeZone)
                    .toDelphiInstant()
                else -> delphiInstant
            }
        }

        fun Instant.toDelphiInstant(): DelphiInstant =
            DelphiInstant(unixToDelphi(toDuration().toDouble(DurationUnit.DAYS)))
    }
}

private val Instant.Companion.ZERO: Instant get() = fromEpochSeconds(0, 0)

private fun Duration.toInstant(): Instant = Instant.ZERO + this
private fun Instant.toDuration(): Duration = epochSeconds.seconds + nanosecondsOfSecond.nanoseconds

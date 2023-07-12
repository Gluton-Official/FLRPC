package delphi

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toDuration
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.DurationUnit
import kotlin.time.toDuration

value class DelphiInstant private constructor(val days: Double) {

    fun toUnixInstant(): Instant = delphiToUnix(days).toDuration(DurationUnit.DAYS).toInstant()

    companion object {
        private fun delphiToUnix(days: Double): Double = days - 25569
        private fun unixToDelphi(days: Double): Double = days + 25569

        /**
         * Creates a UTC [DelphiInstant] from days since the Delphi epoch
         */
        fun fromEpochDays(days: Double, sourceTimeZone: TimeZone = TimeZone.UTC): DelphiInstant =
            DelphiInstant(days)
                .toUnixInstant()
                .toLocalDateTime(TimeZone.UTC)
                .toInstant(sourceTimeZone)
                .toDelphiInstant()

        fun Instant.toDelphiInstant(): DelphiInstant =
            DelphiInstant(unixToDelphi(toDuration().toDouble(DurationUnit.DAYS)))
    }
}
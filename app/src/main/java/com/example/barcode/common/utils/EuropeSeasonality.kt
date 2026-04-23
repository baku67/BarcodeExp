package com.example.barcode.common.utils

import com.example.barcode.features.addItems.manual.ManualSubtypeMeta
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

enum class SeasonRegion {
    EU_NORTH,
    EU_TEMPERATE,
    EU_SOUTH;

    companion object {
        fun fromStorage(value: String?): SeasonRegion {
            return entries.firstOrNull { it.name == value } ?: EU_TEMPERATE
        }
    }
}

enum class EuropeSeason {
    SPRING, SUMMER, AUTUMN, WINTER
}

data class SeasonContext(
    val region: SeasonRegion,
    val currentMonth: Int, // 1..12
    val europeSeason: EuropeSeason
)

object SeasonalityResolver {

    private val defaultZoneId = ZoneId.of("Europe/Paris")

    fun currentMonth(
        zoneId: ZoneId = defaultZoneId,
        clock: Clock = Clock.system(zoneId)
    ): Int = LocalDate.now(clock).monthValue

    fun currentEuropeSeason(month: Int): EuropeSeason = when (month) {
        3, 4, 5 -> EuropeSeason.SPRING
        6, 7, 8 -> EuropeSeason.SUMMER
        9, 10, 11 -> EuropeSeason.AUTUMN
        else -> EuropeSeason.WINTER
    }

    fun currentContext(
        region: SeasonRegion,
        zoneId: ZoneId = defaultZoneId,
        clock: Clock = Clock.system(zoneId)
    ): SeasonContext {
        val month = currentMonth(zoneId, clock)
        return SeasonContext(
            region = region,
            currentMonth = month,
            europeSeason = currentEuropeSeason(month)
        )
    }

    fun regionLabel(region: SeasonRegion): String = when (region) {
        SeasonRegion.EU_NORTH -> "Europe du Nord"
        SeasonRegion.EU_TEMPERATE -> "Europe tempérée"
        SeasonRegion.EU_SOUTH -> "Europe du Sud"
    }

    fun seasonLabel(season: EuropeSeason): String = when (season) {
        EuropeSeason.SPRING -> "Printemps"
        EuropeSeason.SUMMER -> "Été"
        EuropeSeason.AUTUMN -> "Automne"
        EuropeSeason.WINTER -> "Hiver"
    }

    fun monthsFor(
        subtype: ManualSubtypeMeta?,
        region: SeasonRegion
    ): List<Int> {
        return subtype?.seasons
            ?.get(region.name)
            .orEmpty()
            .filter { it in 1..12 }
            .distinct()
            .sorted()
    }

    fun isInSeason(
        subtype: ManualSubtypeMeta?,
        region: SeasonRegion,
        month: Int
    ): Boolean = month in monthsFor(subtype, region)
}
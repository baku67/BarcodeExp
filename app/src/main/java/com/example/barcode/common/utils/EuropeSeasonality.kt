package com.example.barcode.common.utils

import com.example.barcode.features.addItems.manual.ManualSubtypeMeta
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

data class EuropeanCountryOption(
    val code: String,
    val label: String
)

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
    val currentMonth: Int,
    val europeSeason: EuropeSeason
)

private val EUROPEAN_COUNTRIES = listOf(
    EuropeanCountryOption("AL", "Albanie"),
    EuropeanCountryOption("DE", "Allemagne"),
    EuropeanCountryOption("AD", "Andorre"),
    EuropeanCountryOption("AT", "Autriche"),
    EuropeanCountryOption("BE", "Belgique"),
    EuropeanCountryOption("BA", "Bosnie-Herzégovine"),
    EuropeanCountryOption("BG", "Bulgarie"),
    EuropeanCountryOption("HR", "Croatie"),
    EuropeanCountryOption("CY", "Chypre"),
    EuropeanCountryOption("DK", "Danemark"),
    EuropeanCountryOption("ES", "Espagne"),
    EuropeanCountryOption("EE", "Estonie"),
    EuropeanCountryOption("FI", "Finlande"),
    EuropeanCountryOption("FR", "France"),
    EuropeanCountryOption("GR", "Grèce"),
    EuropeanCountryOption("HU", "Hongrie"),
    EuropeanCountryOption("IE", "Irlande"),
    EuropeanCountryOption("IS", "Islande"),
    EuropeanCountryOption("IT", "Italie"),
    EuropeanCountryOption("LV", "Lettonie"),
    EuropeanCountryOption("LI", "Liechtenstein"),
    EuropeanCountryOption("LT", "Lituanie"),
    EuropeanCountryOption("LU", "Luxembourg"),
    EuropeanCountryOption("MT", "Malte"),
    EuropeanCountryOption("MD", "Moldavie"),
    EuropeanCountryOption("MC", "Monaco"),
    EuropeanCountryOption("ME", "Monténégro"),
    EuropeanCountryOption("MK", "Macédoine du Nord"),
    EuropeanCountryOption("NO", "Norvège"),
    EuropeanCountryOption("NL", "Pays-Bas"),
    EuropeanCountryOption("PL", "Pologne"),
    EuropeanCountryOption("PT", "Portugal"),
    EuropeanCountryOption("CZ", "République tchèque"),
    EuropeanCountryOption("RO", "Roumanie"),
    EuropeanCountryOption("GB", "Royaume-Uni"),
    EuropeanCountryOption("SM", "Saint-Marin"),
    EuropeanCountryOption("RS", "Serbie"),
    EuropeanCountryOption("SK", "Slovaquie"),
    EuropeanCountryOption("SI", "Slovénie"),
    EuropeanCountryOption("SE", "Suède"),
    EuropeanCountryOption("CH", "Suisse"),
    EuropeanCountryOption("UA", "Ukraine"),
    EuropeanCountryOption("VA", "Vatican")
).sortedBy { it.label }

object SeasonalityResolver {

    private val defaultZoneId = ZoneId.of("Europe/Paris")

    fun europeanCountries(): List<EuropeanCountryOption> = EUROPEAN_COUNTRIES

    fun isSupportedEuropeanCountryCode(countryCode: String?): Boolean {
        val normalized = countryCode
            ?.trim()
            ?.uppercase(Locale.ROOT)
            .orEmpty()

        return EUROPEAN_COUNTRIES.any { it.code == normalized }
    }

    fun normalizeCountryCodeOrDefault(
        countryCode: String?,
        fallback: String = "FR"
    ): String {
        val normalized = countryCode
            ?.trim()
            ?.uppercase(Locale.ROOT)
            .orEmpty()

        return if (isSupportedEuropeanCountryCode(normalized)) normalized else fallback
    }

    fun defaultCountryCode(): String {
        return normalizeCountryCodeOrDefault(Locale.getDefault().country)
    }

    fun countryLabel(countryCode: String?): String {
        val normalized = normalizeCountryCodeOrDefault(countryCode)
        return EUROPEAN_COUNTRIES.firstOrNull { it.code == normalized }?.label ?: "France"
    }

    fun regionFromCountryCode(countryCode: String?): SeasonRegion {
        return when (normalizeCountryCodeOrDefault(countryCode)) {
            "DK", "EE", "FI", "IS", "LT", "LV", "NO", "SE" -> SeasonRegion.EU_NORTH
            "CY", "ES", "GR", "IT", "MT", "PT" -> SeasonRegion.EU_SOUTH
            else -> SeasonRegion.EU_TEMPERATE
        }
    }

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

    fun currentContextFromCountryCode(
        countryCode: String?,
        zoneId: ZoneId = defaultZoneId,
        clock: Clock = Clock.system(zoneId)
    ): SeasonContext {
        return currentContext(
            region = regionFromCountryCode(countryCode),
            zoneId = zoneId,
            clock = clock
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

    fun monthsFor(
        subtype: ManualSubtypeMeta?,
        countryCode: String?
    ): List<Int> {
        return monthsFor(subtype, regionFromCountryCode(countryCode))
    }

    fun isInSeason(
        subtype: ManualSubtypeMeta?,
        region: SeasonRegion,
        month: Int
    ): Boolean = month in monthsFor(subtype, region)

    fun isInSeason(
        subtype: ManualSubtypeMeta?,
        countryCode: String?,
        month: Int
    ): Boolean = month in monthsFor(subtype, countryCode)
}
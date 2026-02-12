package com.example.barcode.common.expiry

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

const val DEFAULT_SOON_DAYS = 2

enum class ExpiryLevel { NONE, EXPIRED, SOON, OK }

data class ExpiryPolicy(
    val soonDays: Int = DEFAULT_SOON_DAYS,
    val zoneId: ZoneId = ZoneId.systemDefault()
)

fun expiryLocalDate(expiryMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(expiryMillis).atZone(zoneId).toLocalDate()

fun daysUntilExpiry(expiryMillis: Long, policy: ExpiryPolicy = ExpiryPolicy()): Int {
    val today = LocalDate.now(policy.zoneId)
    val target = expiryLocalDate(expiryMillis, policy.zoneId)
    return ChronoUnit.DAYS.between(today, target).toInt()
}

fun expiryLevel(expiryMillis: Long?, policy: ExpiryPolicy = ExpiryPolicy()): ExpiryLevel {
    if (expiryMillis == null) return ExpiryLevel.NONE

    val today = LocalDate.now(policy.zoneId)
    val target = expiryLocalDate(expiryMillis, policy.zoneId)
    val days = ChronoUnit.DAYS.between(today, target).toInt()

    return when {
        target.isBefore(today) -> ExpiryLevel.EXPIRED
        days in 0..policy.soonDays -> ExpiryLevel.SOON
        else -> ExpiryLevel.OK
    }
}

fun formatRelativeDaysCompact(expiryMillis: Long, policy: ExpiryPolicy = ExpiryPolicy()): String {
    val days = daysUntilExpiry(expiryMillis, policy)
    return when {
        days == 0 -> "aujourd'hui"
        days == 1 -> "demain"
        days > 1 -> "dans ${days}j"
        days == -1 -> "hier"
        else -> "il y a ${-days}j"
    }
}

package com.parking.parking_manager.domain.entity

import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime

data class Sector(
    val id: Long,
    val sectorCode: String,
    val basePrice: BigDecimal,
    val maxCapacity: Int,
    val openHour: LocalTime,
    val closeHour: LocalTime,
    val durationLimitMinutes: Int,
    val currencyCode: String = "BRL",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

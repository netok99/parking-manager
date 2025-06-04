package com.parking.parking_manager.domain.entity

import java.time.LocalDateTime

data class Spot(
    val id: Long,
    val sectorId: Long,
    val latitude: Double,
    val longitude: Double,
    val isOccupied: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

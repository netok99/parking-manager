package com.parking.parking_manager.domain.entity

import arrow.core.None
import arrow.core.getOrElse
import arrow.core.some
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDateTime

data class VehicleEvent(
    val id: Long? = null,
    val licensePlate: String,
    val eventType: VehicleStatus = VehicleStatus.ENTERED,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val spotId: Long? = null,
    val sectorId: Long? = null,
    val sectorBasePrice: BigDecimal? = null,
    val dynamicRate: BigDecimal = BigDecimal.ZERO,
    val entryAt: LocalDateTime = LocalDateTime.now(),
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class VehicleStatus {
    ENTERED, PARKED, EXITED
}

private fun isVehicleStatusEntered(vehicleEvent: VehicleEvent) =
    vehicleEvent.eventType == VehicleStatus.ENTERED

private fun isVehicleStatusParked(vehicleEvent: VehicleEvent) =
    vehicleEvent.eventType == VehicleStatus.PARKED

typealias VehicleEvents = List<VehicleEvent>

fun VehicleEvents.filteredStatusEntered() =
    try {
        this.last(::isVehicleStatusEntered).some()
    } catch (_: NoSuchElementException) {
        None
    }

fun VehicleEvents.filteredStatusParked() =
    try {
        this.last(::isVehicleStatusParked).some()
    } catch (_: NoSuchElementException) {
        None
    }

private const val GRACE_PERIOD_MINUTES = 15L
private const val FIRST_HOUR_MINUTES = 60L
private const val BILLING_PERIOD_MINUTES = 15L

fun calculateAmountFromVehicleEventExited(vehicleEvent: VehicleEvent, sector: Sector): BigDecimal {
    if (vehicleEvent.eventType != VehicleStatus.EXITED) return BigDecimal.ZERO
    val parkedMinutes = Duration.between(vehicleEvent.entryAt, vehicleEvent.updatedAt).toMinutes()
    val totalMinutes =
        if (parkedMinutes > sector.durationLimitMinutes) sector.durationLimitMinutes.toLong() else parkedMinutes
    return when {
        totalMinutes <= GRACE_PERIOD_MINUTES -> BigDecimal.ZERO
        else -> {
            val chargeableMinutes = totalMinutes - GRACE_PERIOD_MINUTES
            val chargeableHours = calculateChargeableHours(chargeableMinutes)
            val appliedPrice = when {
                vehicleEvent.dynamicRate < BigDecimal.ZERO -> {
                    vehicleEvent.sectorBasePrice!!.add(
                        vehicleEvent.sectorBasePrice.multiply(
                            vehicleEvent.dynamicRate.divide(BigDecimal(100))
                        )
                    )
                }

                vehicleEvent.dynamicRate > BigDecimal.ZERO -> {
                    val addition = vehicleEvent.dynamicRate.divide(BigDecimal(100)).add(BigDecimal.ONE)
                    vehicleEvent.sectorBasePrice!!.multiply(addition)
                }

                else -> vehicleEvent.sectorBasePrice ?: BigDecimal.ZERO
            }
            appliedPrice
                .multiply(chargeableHours)
                .setScale(2, RoundingMode.HALF_UP)
        }
    }
}

fun calculateCurrentPrice(vehicleEvents: VehicleEvents, currentTime: LocalDateTime, sector: Sector?): BigDecimal {
    if (vehicleEvents.isEmpty()) return BigDecimal.ZERO
    val lastVehicleEvent = vehicleEvents.last()
    if (lastVehicleEvent.eventType != VehicleStatus.PARKED) return BigDecimal.ZERO
    val parkedMinutes = Duration.between(lastVehicleEvent.entryAt, currentTime).toMinutes()
    val totalMinutes = sector?.let { sector ->
        if (parkedMinutes > sector.durationLimitMinutes) sector.durationLimitMinutes.toLong() else parkedMinutes
    } ?: parkedMinutes
    return when {
        totalMinutes <= GRACE_PERIOD_MINUTES -> BigDecimal.ZERO
        else -> {
            val chargeableMinutes = totalMinutes - GRACE_PERIOD_MINUTES
            val chargeableHours = calculateChargeableHours(chargeableMinutes)
            val appliedPrice = getAppliedPriceForVehicle(vehicleEvents)
            appliedPrice
                .multiply(chargeableHours)
                .setScale(2, RoundingMode.HALF_UP)
        }
    }
}

private fun calculateChargeableHours(chargeableMinutes: Long) =
    when {
        chargeableMinutes <= 0 -> BigDecimal.ZERO
        chargeableMinutes <= FIRST_HOUR_MINUTES -> BigDecimal.ONE
        else -> {
            val remainingMinutes = chargeableMinutes - FIRST_HOUR_MINUTES
            val additionalPeriods = (remainingMinutes + BILLING_PERIOD_MINUTES - 1) / BILLING_PERIOD_MINUTES
            val additionalHours = BigDecimal(additionalPeriods)
                .divide(BigDecimal(4), 2, RoundingMode.HALF_UP)
            BigDecimal.ONE.add(additionalHours)
        }
    }

private fun getAppliedPriceForVehicle(vehicleEvents: VehicleEvents): BigDecimal =
    vehicleEvents
        .filteredStatusParked()
        .map { vehicleEventEntered ->
            when {
                vehicleEventEntered.dynamicRate < BigDecimal.ZERO -> {
                    vehicleEventEntered.sectorBasePrice!!.add(
                        vehicleEventEntered.sectorBasePrice.multiply(
                            vehicleEventEntered.dynamicRate.divide(BigDecimal(100))
                        )
                    )
                }

                vehicleEventEntered.dynamicRate > BigDecimal.ZERO -> {
                    val addition = vehicleEventEntered.dynamicRate.divide(BigDecimal(100)).add(BigDecimal.ONE)
                    vehicleEventEntered.sectorBasePrice!!.multiply(addition)
                }

                else -> vehicleEventEntered.sectorBasePrice ?: BigDecimal.ZERO
            }
        }.getOrElse { BigDecimal.ZERO }

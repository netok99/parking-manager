package com.parking.parking_manager.application.service

import com.parking.parking_manager.application.port.input.VehicleEventService
import com.parking.parking_manager.application.port.output.SectorRepository
import com.parking.parking_manager.application.port.output.SpotRepository
import com.parking.parking_manager.application.port.output.VehicleEventRepository
import com.parking.parking_manager.domain.entity.Sector
import com.parking.parking_manager.domain.entity.VehicleEvent
import com.parking.parking_manager.domain.entity.VehicleEvents
import com.parking.parking_manager.domain.entity.VehicleStatus
import com.parking.parking_manager.infrastructure.adapters.model.EntryEventRequest
import com.parking.parking_manager.infrastructure.adapters.model.ExitEventRequest
import com.parking.parking_manager.infrastructure.adapters.model.ParkedEventRequest
import java.math.BigDecimal
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class VehicleEventServiceImpl(
    private val sectorRepository: SectorRepository,
    private val spotRepository: SpotRepository,
    private val vehicleEventRepository: VehicleEventRepository
) : VehicleEventService {

    private val logger = LoggerFactory.getLogger(VehicleEventServiceImpl::class.java)

    override fun handleEntryEvent(event: EntryEventRequest) {
        logger.info("Processing entry event for vehicle: ${event.licensePlate}")
        val vehicleEvents = vehicleEventRepository.getByLicensePlate(event.licensePlate)
        if (vehicleEvents.isNotEmpty() && vehicleEvents.last().eventType != VehicleStatus.EXITED) return
        vehicleEventRepository.save(
            VehicleEvent(
                licensePlate = event.licensePlate,
                eventType = VehicleStatus.ENTERED,
                entryAt = event.entryTime
            )
        )
    }

    override fun handleParkedEvent(event: ParkedEventRequest) {
        logger.info("Processing parked event for vehicle: ${event.licensePlate}")
        val vehicleEvents = vehicleEventRepository.getByLicensePlate(event.licensePlate)
        if (vehicleEvents.isNotEmpty() && vehicleEvents.last().eventType != VehicleStatus.ENTERED) return
        val spot = spotRepository.findByCoordinates(
            latitude = event.latitude,
            longitude = event.longitude
        )
        val sector = sectorRepository.getById(spot.sectorId)
        vehicleEventRepository.save(
            VehicleEvent(
                licensePlate = event.licensePlate,
                eventType = VehicleStatus.PARKED,
                latitude = event.latitude,
                longitude = event.longitude,
                spotId = spot.id,
                sectorId = sector.id,
                sectorBasePrice = sector.basePrice,
                dynamicRate = calculatedDynamicRate(
                    sector = sector,
                    vehicleEvents = vehicleEvents
                ),
                entryAt = vehicleEvents.last().entryAt
            )
        )
    }

    fun calculatedDynamicRate(sector: Sector, vehicleEvents: VehicleEvents): BigDecimal {
        val vehicleEventsParked = vehicleEvents.filter { it.eventType == VehicleStatus.PARKED }
        val vehicleEventsExited = vehicleEvents.filter { it.eventType == VehicleStatus.EXITED }
        val quantityParkedVehicles = vehicleEventsParked.size - vehicleEventsExited.size
        val occupancyPercentage = quantityParkedVehicles * 100 / sector.maxCapacity
        return BigDecimal(
            when {
                occupancyPercentage < 25 -> -10
                occupancyPercentage <= 50 -> 0
                occupancyPercentage <= 75 -> 10
                else -> 25
            }
        )
    }

    override fun handleExitEvent(event: ExitEventRequest) {
        val vehicleEvents = vehicleEventRepository.getByLicensePlate(event.licensePlate)
        if (vehicleEvents.isNotEmpty() && vehicleEvents.last().eventType != VehicleStatus.PARKED) return
        val parkedVehicleEvent = vehicleEvents.last()
        vehicleEventRepository.save(
            VehicleEvent(
                licensePlate = event.licensePlate,
                eventType = VehicleStatus.EXITED,
                latitude = parkedVehicleEvent.latitude,
                longitude = parkedVehicleEvent.longitude,
                spotId = parkedVehicleEvent.spotId,
                sectorId = parkedVehicleEvent.sectorId,
                sectorBasePrice = parkedVehicleEvent.sectorBasePrice,
                dynamicRate = parkedVehicleEvent.dynamicRate,
                entryAt = vehicleEvents.last().entryAt,
                updatedAt = event.exitTime
            )
        )
    }
}

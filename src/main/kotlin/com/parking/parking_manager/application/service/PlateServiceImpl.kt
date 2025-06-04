package com.parking.parking_manager.application.service

import com.parking.parking_manager.application.port.input.PlateService
import com.parking.parking_manager.application.port.output.SectorRepository
import com.parking.parking_manager.application.port.output.VehicleEventRepository
import com.parking.parking_manager.domain.entity.calculateCurrentPrice
import com.parking.parking_manager.infrastructure.adapters.model.PlateStatusResponse
import java.time.LocalDateTime
import org.springframework.stereotype.Service

@Service
class PlateServiceImpl(
    private val sectorRepository: SectorRepository,
    private val vehicleEventRepository: VehicleEventRepository
) : PlateService {

    override fun getPlateStatus(licensePlate: String): PlateStatusResponse {
        val vehicleEvents = vehicleEventRepository.getByLicensePlate(licensePlate)
        val lastVehicleEvent = runCatching { vehicleEvents.last() }
        val entryTime = lastVehicleEvent.map { it.entryAt }.getOrNull()
        val timeParked = lastVehicleEvent.map { it.updatedAt }.getOrNull()
        val sector = lastVehicleEvent
            .map {
                it.sectorId?.let { sectorId -> runCatching { sectorRepository.getById(sectorId) }.getOrNull() }
            }.getOrNull()
        return PlateStatusResponse(
            licensePlate = licensePlate,
            priceUntilNow = calculateCurrentPrice(
                vehicleEvents = vehicleEvents,
                currentTime = LocalDateTime.now(),
                sector = sector
            ),
            entryTime = entryTime,
            timeParked = timeParked
        )
    }
}

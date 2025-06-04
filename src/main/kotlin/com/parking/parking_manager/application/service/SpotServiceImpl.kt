package com.parking.parking_manager.application.service

import com.parking.parking_manager.application.port.input.SpotService
import com.parking.parking_manager.application.port.output.SpotRepository
import com.parking.parking_manager.application.port.output.VehicleEventRepository
import com.parking.parking_manager.domain.entity.VehicleStatus
import com.parking.parking_manager.infrastructure.adapters.model.SpotStatusResponse
import org.springframework.stereotype.Service

@Service
class SpotServiceImpl(
    private val spotRepository: SpotRepository,
    private val vehicleEventRepository: VehicleEventRepository
) : SpotService {

    override fun getSpotStatus(latitude: Double, longitude: Double): SpotStatusResponse {
        val spot = spotRepository.findByCoordinates(latitude = latitude, longitude = longitude)
        val lastVehiclesEvents = vehicleEventRepository.getBySpotId(spot.id)
        val lastVehicleEvent = runCatching { lastVehiclesEvents.last() }
        val occupied = lastVehicleEvent
            .map { it.eventType == VehicleStatus.PARKED }
            .getOrDefault(false)
        val entryTime = lastVehicleEvent.map { it.entryAt }.getOrNull()
        val timeParked = lastVehicleEvent.map { it.updatedAt }.getOrNull()
        return SpotStatusResponse(occupied = occupied, entryTime = entryTime, timeParked = timeParked)
    }
}

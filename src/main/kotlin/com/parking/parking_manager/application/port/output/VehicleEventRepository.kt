package com.parking.parking_manager.application.port.output

import com.parking.parking_manager.domain.entity.VehicleEvent
import com.parking.parking_manager.domain.entity.VehicleEvents

interface VehicleEventRepository {
    fun getByLicensePlate(licensePlate: String): VehicleEvents
    fun getBySpotId(spotId: Long): VehicleEvents
    fun getBySectorIdAndDate(sectorId: Long, date: String): VehicleEvents
    fun save(vehicleEvent: VehicleEvent)
}

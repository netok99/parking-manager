package com.parking.parking_manager.application.port.input

import com.parking.parking_manager.infrastructure.adapters.model.PlateStatusResponse

interface PlateService {
    fun getPlateStatus(licensePlate: String): PlateStatusResponse
}

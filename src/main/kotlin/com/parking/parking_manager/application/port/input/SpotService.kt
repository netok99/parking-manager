package com.parking.parking_manager.application.port.input

import com.parking.parking_manager.infrastructure.adapters.model.SpotStatusResponse

interface SpotService {
    fun getSpotStatus(latitude: Double, longitude: Double): SpotStatusResponse
}

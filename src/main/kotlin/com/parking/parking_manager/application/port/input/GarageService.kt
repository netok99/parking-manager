package com.parking.parking_manager.application.port.input

import com.parking.parking_manager.infrastructure.adapters.model.CreateGarageRequest

interface GarageService {
    fun createGarage(createGarageRequest: CreateGarageRequest)
}

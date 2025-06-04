package com.parking.parking_manager.application.port.input

import com.parking.parking_manager.infrastructure.adapters.model.RevenueResponse

interface RevenueService {
    fun getRevenue(date: String, sectorCode: String): RevenueResponse
}

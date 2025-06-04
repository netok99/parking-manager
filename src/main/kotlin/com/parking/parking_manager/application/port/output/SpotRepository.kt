package com.parking.parking_manager.application.port.output

import com.parking.parking_manager.domain.entity.Spot

interface SpotRepository {
    fun findByCoordinates(latitude: Double, longitude: Double): Spot
    fun findById(id: Long): Spot
    fun findBySector(sector: String): List<Spot>
    fun save(spot: Spot): Spot
    fun deleteAll()
}

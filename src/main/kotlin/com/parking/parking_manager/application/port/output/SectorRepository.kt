package com.parking.parking_manager.application.port.output

import com.parking.parking_manager.domain.entity.Sector

interface SectorRepository {
    fun getById(sectorId: Long): Sector
    fun getByCode(sectorCode: String): Sector
    fun save(sector: Sector)
    fun deleteAll()
}

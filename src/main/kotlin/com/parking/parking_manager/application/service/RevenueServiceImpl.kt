package com.parking.parking_manager.application.service

import com.parking.parking_manager.application.port.input.RevenueService
import com.parking.parking_manager.application.port.output.SectorRepository
import com.parking.parking_manager.application.port.output.VehicleEventRepository
import com.parking.parking_manager.domain.entity.VehicleStatus
import com.parking.parking_manager.domain.entity.calculateAmountFromVehicleEventExited
import com.parking.parking_manager.infrastructure.adapters.model.RevenueResponse
import java.math.BigDecimal
import java.time.LocalDateTime
import org.springframework.stereotype.Service

@Service
class RevenueServiceImpl(
    private val sectorRepository: SectorRepository,
    private val vehicleEventRepository: VehicleEventRepository
) : RevenueService {

    override fun getRevenue(date: String, sectorCode: String): RevenueResponse {
        val sector = sectorRepository.getByCode(sectorCode)
        val totalAmount = vehicleEventRepository
            .getBySectorIdAndDate(sectorId = sector.id, date = date)
            .filter { vehicleEvent -> vehicleEvent.eventType == VehicleStatus.EXITED }
            .fold(BigDecimal.ZERO) { acc, vehicleEvent ->
                acc.add(calculateAmountFromVehicleEventExited(vehicleEvent, sector))
            }
        return RevenueResponse(
            amount = totalAmount,
            currency = sector.currencyCode,
            timestamp = LocalDateTime.now()
        )
    }
}

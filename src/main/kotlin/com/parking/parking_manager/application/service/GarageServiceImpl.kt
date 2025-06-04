package com.parking.parking_manager.application.service

import com.parking.parking_manager.application.port.input.GarageService
import com.parking.parking_manager.application.port.output.SectorRepository
import com.parking.parking_manager.application.port.output.SpotRepository
import com.parking.parking_manager.domain.entity.Sector
import com.parking.parking_manager.domain.entity.Spot
import com.parking.parking_manager.infrastructure.adapters.model.CreateGarageRequest
import com.parking.parking_manager.infrastructure.exception.SpotNotFoundException
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GarageServiceImpl(
    private val sectorRepository: SectorRepository,
    private val spotRepository: SpotRepository
) : GarageService {

    private val logger = LoggerFactory.getLogger(GarageServiceImpl::class.java)

    override fun createGarage(createGarageRequest: CreateGarageRequest) {
        clearPersistedDataFromSectorsAndSpots()
        val sectors = createSectors(createGarageRequest)
        createSpots(createGarageRequest, sectors)
    }

    private fun clearPersistedDataFromSectorsAndSpots() {
        spotRepository.deleteAll()
        sectorRepository.deleteAll()
        logger.info("Deleted data from spots and sectors")
    }

    private fun createSectors(createGarageRequest: CreateGarageRequest): List<Sector> {
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val sectors = createGarageRequest
            .garage
            .map {
                Sector(
                    id = 1L,
                    sectorCode = it.sector,
                    basePrice = it.basePrice,
                    maxCapacity = it.maxCapacity,
                    openHour = LocalTime.parse(it.openHour, formatter),
                    closeHour = LocalTime.parse(it.closeHour, formatter),
                    durationLimitMinutes = it.durationLimitMinutes,
                    currencyCode = "BRL",
                    createdAt = LocalDateTime.now(),
                    updatedAt = LocalDateTime.now()
                )
            }
        sectors.map(sectorRepository::save)
        logger.info("Sectors are persisted")
        return sectors
    }

    private fun createSpots(createGarageRequest: CreateGarageRequest, sectors: List<Sector>) {
        val sectorsCodeId = sectors
            .map {
                val sectorFromRepository = sectorRepository.getByCode(it.sectorCode)
                mapOf(sectorFromRepository.sectorCode to sectorFromRepository.id)
            }
            .flatMap { it.entries }
            .associate { it.key to it.value }

        val spots = createGarageRequest.spots.map {
            Spot(
                id = 1,
                sectorId = sectorsCodeId[it.sector] ?: throw SpotNotFoundException("Spot not found"),
                latitude = it.lat,
                longitude = it.lng,
                isOccupied = false
            )
        }
        spots.map(spotRepository::save)
        logger.info("Spots are persisted")
    }
}

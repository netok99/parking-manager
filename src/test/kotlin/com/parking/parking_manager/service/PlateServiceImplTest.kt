package com.parking.parking_manager.service

import com.parking.parking_manager.application.port.output.SectorRepository
import com.parking.parking_manager.application.port.output.VehicleEventRepository
import com.parking.parking_manager.application.service.PlateServiceImpl
import com.parking.parking_manager.domain.entity.VehicleEvent
import com.parking.parking_manager.domain.entity.VehicleStatus
import io.mockk.every
import io.mockk.mockk
import java.math.BigDecimal
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

class PlateServiceImplTest {

    private val sectorRepository = mockk<SectorRepository>()
    private val vehicleEventRepository = mockk<VehicleEventRepository>()
    private val plateService = PlateServiceImpl(sectorRepository, vehicleEventRepository)

    private val licensePlate = "ABC1234"
    private val baseTime = LocalDateTime.of(2025, 6, 3, 10, 0, 0)
    private val basePrice = BigDecimal("10.00")
    private val spotId = 1L
    private val sectorId = 1L

    @BeforeEach
    fun setUp() {
        every { vehicleEventRepository.getByLicensePlate(any()) } returns emptyList()
    }

    @Nested
    @DisplayName("Cenários sem eventos")
    inner class EmptyEventsTests {

        @Test
        @DisplayName("Deve retornar resposta padrão quando não há eventos para a placa")
        fun `should return default response when no events exist for license plate`() {
            every { vehicleEventRepository.getByLicensePlate(licensePlate) } returns emptyList()

            val result = plateService.getPlateStatus(licensePlate)

            assertEquals(licensePlate, result.licensePlate)
            assertEquals(BigDecimal.ZERO, result.priceUntilNow)
            assertNull(result.entryTime)
            assertNull(result.timeParked)
        }
    }

    @Nested
    @DisplayName("Cenários com saída")
    inner class ExitEventsTests {

        @Test
        @DisplayName("Deve calcular preço final quando veículo já saiu")
        fun `should calculate final price when vehicle has exited`() {
            val entryTime = baseTime
            val parkedTime = baseTime.plusMinutes(5)
            val exitTime = baseTime.plusMinutes(90) // 1h30min total

            val events = listOf(
                createVehicleEvent(VehicleStatus.ENTERED, entryTime),
                createVehicleEvent(VehicleStatus.PARKED, parkedTime),
                createVehicleEvent(VehicleStatus.EXITED, exitTime)
            )

            every { vehicleEventRepository.getByLicensePlate(licensePlate) } returns events

            val result = plateService.getPlateStatus(licensePlate)

            assertEquals(BigDecimal.ZERO, result.priceUntilNow)
        }
    }

    private fun createVehicleEvent(
        status: VehicleStatus,
        time: LocalDateTime,
        dynamicRate: BigDecimal = BigDecimal.ZERO
    ): VehicleEvent {
        return VehicleEvent(
            id = 1L,
            licensePlate = licensePlate,
            eventType = status,
            latitude = -23.561684,
            longitude = -46.655981,
            spotId = spotId,
            sectorId = sectorId,
            sectorBasePrice = basePrice,
            dynamicRate = dynamicRate,
            createdAt = time,
            updatedAt = time
        )
    }
}
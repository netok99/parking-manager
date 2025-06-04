package com.parking.parking_manager.service

import com.parking.parking_manager.application.port.output.SectorRepository
import com.parking.parking_manager.application.port.output.VehicleEventRepository
import com.parking.parking_manager.application.service.RevenueServiceImpl
import com.parking.parking_manager.domain.entity.Sector
import com.parking.parking_manager.domain.entity.VehicleEvent
import com.parking.parking_manager.domain.entity.VehicleStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RevenueServiceImplTest {

    private lateinit var sectorRepository: SectorRepository
    private lateinit var vehicleEventRepository: VehicleEventRepository
    private lateinit var revenueService: RevenueServiceImpl

    private val testSector = Sector(
        id = 1L,
        sectorCode = "A",
        basePrice = BigDecimal("10.00"),
        maxCapacity = 100,
        openHour = LocalTime.of(8, 0),
        closeHour = LocalTime.of(22, 0),
        durationLimitMinutes = 240,
        currencyCode = "BRL"
    )

    @BeforeEach
    fun setUp() {
        sectorRepository = mockk()
        vehicleEventRepository = mockk()
        revenueService = RevenueServiceImpl(sectorRepository, vehicleEventRepository)
    }

    @Test
    fun `should return zero revenue when no exited vehicles in date and sector`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val emptyVehicleEvents = emptyList<VehicleEvent>()

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns emptyVehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        assertEquals(BigDecimal.ZERO, result.amount)
        assertEquals("BRL", result.currency)
        verify { sectorRepository.getByCode(sectorCode) }
        verify { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) }
    }

    @Test
    fun `should return zero revenue when only entry and parked events exist`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val vehicleEvents = listOf(
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.ENTERED,
                entryAt = LocalDateTime.of(2025, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2025, 1, 1, 10, 0)
            ),
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.PARKED,
                entryAt = LocalDateTime.of(2025, 1, 1, 10, 0),
                updatedAt = LocalDateTime.of(2025, 1, 1, 10, 5)
            )
        )

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns vehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        assertEquals(BigDecimal.ZERO, result.amount)
        assertEquals("BRL", result.currency)
    }

    @Test
    fun `should return zero revenue for vehicle parked within grace period (15 minutes)`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val entryTime = LocalDateTime.of(2025, 1, 1, 10, 0)
        val exitTime = entryTime.plusMinutes(10) // 10 minutes - within grace period

        val vehicleEvents = listOf(
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00")
            )
        )

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns vehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        assertEquals(BigDecimal.ZERO, result.amount)
        assertEquals("BRL", result.currency)
    }

    @Test
    fun `should calculate correct revenue for single vehicle parked for first hour`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val entryTime = LocalDateTime.of(2025, 1, 1, 10, 0)
        val exitTime = entryTime.plusMinutes(75) // 75 minutes = 60 minutes chargeable after grace period

        val vehicleEvents = listOf(
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00")
            )
        )

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns vehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        assertEquals(BigDecimal("10.00"), result.amount)
        assertEquals("BRL", result.currency)
    }

    @Test
    fun `should calculate correct revenue for vehicle parked beyond first hour with pro-rata billing`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val entryTime = LocalDateTime.of(2025, 1, 1, 10, 0)
        val exitTime = entryTime.plusMinutes(105) // 90 minutes chargeable (60 + 30 minutes)

        val vehicleEvents = listOf(
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00")
            )
        )

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns vehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        // First hour (10.00) + 30 minutes pro-rata (2 periods of 15 min = 0.5 hour = 5.00) = 15.00
        assertEquals(BigDecimal("15.00"), result.amount)
        assertEquals("BRL", result.currency)
    }

    @Test
    fun `should apply discount for dynamic rate less than zero`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val entryTime = LocalDateTime.of(2025, 1, 1, 10, 0)
        val exitTime = entryTime.plusMinutes(75) // 60 minutes chargeable

        val vehicleEvents = listOf(
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal("-10") // 10% discount
            )
        )

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns vehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        // Base price 10.00 with 10% discount = 9.00
        assertEquals(BigDecimal("9.00"), result.amount)
        assertEquals("BRL", result.currency)
    }

    @Test
    fun `should apply surcharge for dynamic rate greater than zero`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val entryTime = LocalDateTime.of(2025, 1, 1, 10, 0)
        val exitTime = entryTime.plusMinutes(75) // 60 minutes chargeable

        val vehicleEvents = listOf(
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal("25") // 25% surcharge
            )
        )

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns vehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        // Base price 10.00 with 25% surcharge = 12.50
        assertEquals(BigDecimal("12.50"), result.amount)
        assertEquals("BRL", result.currency)
    }

    @Test
    fun `should calculate total revenue for multiple vehicles`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val entryTime = LocalDateTime.of(2025, 1, 1, 10, 0)

        val vehicleEvents = listOf(
            // Vehicle 1: 1 hour, no dynamic rate
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = entryTime.plusMinutes(75),
                sectorBasePrice = BigDecimal("10.00")
            ),
            // Vehicle 2: 1.5 hours, 10% discount
            createVehicleEvent(
                licensePlate = "DEF5678",
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = entryTime.plusMinutes(105),
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal("-10")
            ),
            // Vehicle 3: Still parked (ENTERED event - should not be counted)
            createVehicleEvent(
                licensePlate = "GHI9012",
                eventType = VehicleStatus.ENTERED,
                entryAt = entryTime,
                updatedAt = entryTime,
                sectorBasePrice = BigDecimal("10.00")
            )
        )

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns vehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        // Vehicle 1: 10.00
        // Vehicle 2: 9.00 * 1.5 = 13.50
        // Vehicle 3: Not counted (not EXITED)
        // Total: 10.00 + 13.50 = 23.50
        assertEquals(BigDecimal("23.50"), result.amount)
        assertEquals("BRL", result.currency)
    }

    @Test
    fun `should use correct currency from sector`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val usdSector = testSector.copy(currencyCode = "USD")
        val emptyVehicleEvents = emptyList<VehicleEvent>()

        every { sectorRepository.getByCode(sectorCode) } returns usdSector
        every { vehicleEventRepository.getBySectorIdAndDate(usdSector.id, date) } returns emptyVehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        assertEquals("USD", result.currency)
    }

    @Test
    fun `should handle vehicles with null sector base price`() {
        val date = "2025-01-01"
        val sectorCode = "A"
        val entryTime = LocalDateTime.of(2025, 1, 1, 10, 0)
        val exitTime = entryTime.plusMinutes(75)

        val vehicleEvents = listOf(
            createVehicleEvent(
                licensePlate = "ABC1234",
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = null // Null base price
            )
        )

        every { sectorRepository.getByCode(sectorCode) } returns testSector
        every { vehicleEventRepository.getBySectorIdAndDate(testSector.id, date) } returns vehicleEvents

        val result = revenueService.getRevenue(date, sectorCode)

        assertEquals(BigDecimal("0.00"), result.amount)
        assertEquals("BRL", result.currency)
    }

    private fun createVehicleEvent(
        licensePlate: String,
        eventType: VehicleStatus,
        entryAt: LocalDateTime,
        updatedAt: LocalDateTime,
        sectorBasePrice: BigDecimal? = null,
        dynamicRate: BigDecimal = BigDecimal.ZERO
    ) = VehicleEvent(
        id = 1L,
        licensePlate = licensePlate,
        eventType = eventType,
        latitude = -23.561684,
        longitude = -46.655981,
        spotId = 1L,
        sectorId = 1L,
        sectorBasePrice = sectorBasePrice,
        dynamicRate = dynamicRate,
        entryAt = entryAt,
        createdAt = updatedAt,
        updatedAt = updatedAt
    )
}

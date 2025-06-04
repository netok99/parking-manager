package com.parking.parking_manager.service

import com.parking.parking_manager.application.port.output.SectorRepository
import com.parking.parking_manager.application.port.output.SpotRepository
import com.parking.parking_manager.application.service.GarageServiceImpl
import com.parking.parking_manager.domain.entity.Sector
import com.parking.parking_manager.domain.entity.Spot
import com.parking.parking_manager.infrastructure.adapters.model.CreateGarageRequest
import com.parking.parking_manager.infrastructure.adapters.model.GarageRequest
import com.parking.parking_manager.infrastructure.adapters.model.SpotsRequest
import com.parking.parking_manager.infrastructure.exception.SpotNotFoundException
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalTime
import kotlin.test.assertEquals

class GarageServiceImplTest {

    private val sectorRepository = mockk<SectorRepository>()
    private val spotRepository = mockk<SpotRepository>()
    private lateinit var garageService: GarageServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        garageService = GarageServiceImpl(sectorRepository, spotRepository)

        every { spotRepository.deleteAll() } just Runs
        every { sectorRepository.deleteAll() } just Runs
        every { sectorRepository.save(any()) } just Runs
        every { spotRepository.save(any()) } returns mockk()
    }

    @Test
    fun `should create garage successfully with single sector and spot`() {
        val garageRequest = createSingleSectorGarageRequest()
        val savedSector = createMockSector("A", 1L)
        every { sectorRepository.getByCode("A") } returns savedSector
        garageService.createGarage(garageRequest)

        verifyRepositoryCalls()
        verify(exactly = 1) { sectorRepository.save(any()) }
        verify(exactly = 1) { spotRepository.save(any()) }
    }

    @Test
    fun `should create garage successfully with multiple sectors and spots`() {
        val garageRequest = createMultipleSectorGarageRequest()
        val sectorA = createMockSector("A", 1L)
        val sectorB = createMockSector("B", 2L)

        every { sectorRepository.getByCode("A") } returns sectorA
        every { sectorRepository.getByCode("B") } returns sectorB
        garageService.createGarage(garageRequest)

        verifyRepositoryCalls()
        verify(exactly = 2) { sectorRepository.save(any()) }
        verify(exactly = 3) { spotRepository.save(any()) }
    }

    @Test
    fun `should clear existing data before creating new garage`() {
        val garageRequest = createSingleSectorGarageRequest()
        val savedSector = createMockSector("A", 1L)

        every { sectorRepository.getByCode("A") } returns savedSector
        garageService.createGarage(garageRequest)

        verifyOrder {
            spotRepository.deleteAll()
            sectorRepository.deleteAll()
            sectorRepository.save(any())
            spotRepository.save(any())
        }
    }

    @Test
    fun `should create sectors with correct attributes`() {
        val garageRequest = createDetailedSectorGarageRequest()
        val savedSector = createMockSector("PREMIUM", 1L)

        every { sectorRepository.getByCode("PREMIUM") } returns savedSector
        val sectorSlot = slot<Sector>()
        every { sectorRepository.save(capture(sectorSlot)) } just Runs
        garageService.createGarage(garageRequest)

        val capturedSector = sectorSlot.captured
        assertEquals("PREMIUM", capturedSector.sectorCode)
        assertEquals(BigDecimal("15.50"), capturedSector.basePrice)
        assertEquals(50, capturedSector.maxCapacity)
        assertEquals(LocalTime.of(6, 30), capturedSector.openHour)
        assertEquals(LocalTime.of(23, 45), capturedSector.closeHour)
        assertEquals(300, capturedSector.durationLimitMinutes)
        assertEquals("BRL", capturedSector.currencyCode)
    }

    @Test
    fun `should create spots with correct sector mapping`() {
        val garageRequest = createMultipleSectorGarageRequest()
        val sectorA = createMockSector("A", 10L)
        val sectorB = createMockSector("B", 20L)

        every { sectorRepository.getByCode("A") } returns sectorA
        every { sectorRepository.getByCode("B") } returns sectorB

        val spotSlots = mutableListOf<Spot>()
        every { spotRepository.save(capture(spotSlots)) } returns mockk()

        garageService.createGarage(garageRequest)

        assertEquals(3, spotSlots.size)

        val spotA = spotSlots.find { it.latitude == -23.561684 }!!
        assertEquals(10L, spotA.sectorId)
        assertEquals(-46.655981, spotA.longitude)
        assertEquals(false, spotA.isOccupied)

        val spotB1 = spotSlots.find { it.latitude == -23.561674 }!!
        assertEquals(20L, spotB1.sectorId)

        val spotB2 = spotSlots.find { it.latitude == -23.561664 }!!
        assertEquals(20L, spotB2.sectorId)
    }

    @Test
    fun `should throw SpotNotFoundException when sector not found for spot`() {
        val garageRequest = createSingleSectorGarageRequest()
        every { sectorRepository.getByCode("A") } throws SpotNotFoundException("Sector not found")

        assertThrows<SpotNotFoundException> {
            garageService.createGarage(garageRequest)
        }

        verify { sectorRepository.save(any()) }
        verify(exactly = 0) { spotRepository.save(any()) }
    }

    @Test
    fun `should handle edge case time formats correctly`() {
        val garageRequest = CreateGarageRequest(
            garage = listOf(
                GarageRequest(
                    sector = "TEST",
                    basePrice = BigDecimal("10.00"),
                    maxCapacity = 1,
                    openHour = "00:00",
                    closeHour = "23:59",
                    durationLimitMinutes = 1440
                )
            ),
            spots = listOf(
                SpotsRequest(id = 1, sector = "TEST", lat = 0.0, lng = 0.0)
            )
        )

        val savedSector = createMockSector("TEST", 1L)
        every { sectorRepository.getByCode("TEST") } returns savedSector

        val sectorSlot = slot<Sector>()
        every { sectorRepository.save(capture(sectorSlot)) } just Runs

        garageService.createGarage(garageRequest)

        val capturedSector = sectorSlot.captured
        assertEquals(LocalTime.of(0, 0), capturedSector.openHour)
        assertEquals(LocalTime.of(23, 59), capturedSector.closeHour)
    }

    @Test
    fun `should handle spots with extreme coordinate values`() {
        val garageRequest = CreateGarageRequest(
            garage = listOf(
                GarageRequest(
                    sector = "EXTREME",
                    basePrice = BigDecimal("5.00"),
                    maxCapacity = 2,
                    openHour = "08:00",
                    closeHour = "18:00",
                    durationLimitMinutes = 120
                )
            ),
            spots = listOf(
                SpotsRequest(id = 1, sector = "EXTREME", lat = -90.0, lng = -180.0),
                SpotsRequest(id = 2, sector = "EXTREME", lat = 90.0, lng = 180.0)
            )
        )

        val savedSector = createMockSector("EXTREME", 1L)
        every { sectorRepository.getByCode("EXTREME") } returns savedSector

        val spotSlots = mutableListOf<Spot>()
        every { spotRepository.save(capture(spotSlots)) } returns mockk()

        garageService.createGarage(garageRequest)

        assertEquals(2, spotSlots.size)
        val extremeSpot1 = spotSlots.find { it.latitude == -90.0 }!!
        assertEquals(-180.0, extremeSpot1.longitude)

        val extremeSpot2 = spotSlots.find { it.latitude == 90.0 }!!
        assertEquals(180.0, extremeSpot2.longitude)
    }

    @Test
    fun `should handle large number of sectors and spots`() {
        val sectors = (1..10).map { i ->
            GarageRequest(
                sector = "SECTOR_$i",
                basePrice = BigDecimal("${i * 2}.00"),
                maxCapacity = i * 10,
                openHour = "08:00",
                closeHour = "20:00",
                durationLimitMinutes = 240
            )
        }

        val spots = (1..50).map { i ->
            SpotsRequest(
                id = i,
                sector = "SECTOR_${(i - 1) / 5 + 1}",
                lat = -23.0 + (i * 0.001),
                lng = -46.0 + (i * 0.001)
            )
        }

        val garageRequest = CreateGarageRequest(garage = sectors, spots = spots)

        (1..10).forEach { i ->
            every { sectorRepository.getByCode("SECTOR_$i") } returns
                    createMockSector("SECTOR_$i", i.toLong())
        }

        garageService.createGarage(garageRequest)

        verify(exactly = 10) { sectorRepository.save(any()) }
        verify(exactly = 50) { spotRepository.save(any()) }
    }

    private fun verifyRepositoryCalls() {
        verify { spotRepository.deleteAll() }
        verify { sectorRepository.deleteAll() }
    }

    private fun createSingleSectorGarageRequest() = CreateGarageRequest(
        garage = listOf(
            GarageRequest(
                sector = "A",
                basePrice = BigDecimal("10.00"),
                maxCapacity = 100,
                openHour = "08:00",
                closeHour = "22:00",
                durationLimitMinutes = 240
            )
        ),
        spots = listOf(
            SpotsRequest(id = 1, sector = "A", lat = -23.561684, lng = -46.655981)
        )
    )

    private fun createMultipleSectorGarageRequest() = CreateGarageRequest(
        garage = listOf(
            GarageRequest(
                sector = "A",
                basePrice = BigDecimal("10.00"),
                maxCapacity = 100,
                openHour = "08:00",
                closeHour = "22:00",
                durationLimitMinutes = 240
            ),
            GarageRequest(
                sector = "B",
                basePrice = BigDecimal("4.00"),
                maxCapacity = 72,
                openHour = "05:00",
                closeHour = "18:00",
                durationLimitMinutes = 120
            )
        ),
        spots = listOf(
            SpotsRequest(id = 1, sector = "A", lat = -23.561684, lng = -46.655981),
            SpotsRequest(id = 2, sector = "B", lat = -23.561674, lng = -46.655971),
            SpotsRequest(id = 3, sector = "B", lat = -23.561664, lng = -46.655961)
        )
    )

    private fun createDetailedSectorGarageRequest() = CreateGarageRequest(
        garage = listOf(
            GarageRequest(
                sector = "PREMIUM",
                basePrice = BigDecimal("15.50"),
                maxCapacity = 50,
                openHour = "06:30",
                closeHour = "23:45",
                durationLimitMinutes = 300
            )
        ),
        spots = listOf(
            SpotsRequest(id = 1, sector = "PREMIUM", lat = -23.561684, lng = -46.655981)
        )
    )

    private fun createMockSector(code: String, id: Long) = Sector(
        id = id,
        sectorCode = code,
        basePrice = BigDecimal("10.00"),
        maxCapacity = 100,
        openHour = LocalTime.of(8, 0),
        closeHour = LocalTime.of(22, 0),
        durationLimitMinutes = 240,
        currencyCode = "BRL"
    )
}
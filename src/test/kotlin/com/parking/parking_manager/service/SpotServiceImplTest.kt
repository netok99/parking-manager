package com.parking.parking_manager.service

import com.parking.parking_manager.application.port.output.SpotRepository
import com.parking.parking_manager.application.port.output.VehicleEventRepository
import com.parking.parking_manager.application.service.SpotServiceImpl
import com.parking.parking_manager.domain.entity.Spot
import com.parking.parking_manager.domain.entity.VehicleEvent
import com.parking.parking_manager.domain.entity.VehicleStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows

class SpotServiceImplTest {

    private lateinit var spotRepository: SpotRepository
    private lateinit var vehicleEventRepository: VehicleEventRepository
    private lateinit var spotService: SpotServiceImpl

    private val testLatitude = -23.561684
    private val testLongitude = -46.655981
    private val testSpotId = 1L
    private val testSectorId = 1L
    private val testLicensePlate = "ABC1234"

    @BeforeEach
    fun setUp() {
        spotRepository = mockk()
        vehicleEventRepository = mockk()
        spotService = SpotServiceImpl(spotRepository, vehicleEventRepository)
    }

    @Test
    @DisplayName("Should return occupied spot when last event is PARKED")
    fun `should return occupied spot when last event is parked`() {
        val spot = createTestSpot()
        val entryTime = LocalDateTime.now().minusHours(1)
        val parkedTime = LocalDateTime.now().minusMinutes(30)

        val vehicleEvents = listOf(
            createVehicleEvent(
                eventType = VehicleStatus.ENTERED,
                entryAt = entryTime,
                updatedAt = entryTime
            ),
            createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = entryTime,
                updatedAt = parkedTime
            )
        )

        every { spotRepository.findByCoordinates(testLatitude, testLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns vehicleEvents

        val result = spotService.getSpotStatus(testLatitude, testLongitude)

        assertTrue(result.occupied)
        assertEquals(entryTime, result.entryTime)
        assertEquals(parkedTime, result.timeParked)

        verify { spotRepository.findByCoordinates(testLatitude, testLongitude) }
        verify { vehicleEventRepository.getBySpotId(testSpotId) }
    }

    @Test
    @DisplayName("Should return unoccupied spot when last event is EXITED")
    fun `should return unoccupied spot when last event is exited`() {
        val spot = createTestSpot()
        val entryTime = LocalDateTime.now().minusHours(2)
        val parkedTime = LocalDateTime.now().minusHours(1)
        val exitTime = LocalDateTime.now().minusMinutes(30)

        val vehicleEvents = listOf(
            createVehicleEvent(
                eventType = VehicleStatus.ENTERED,
                entryAt = entryTime,
                updatedAt = entryTime
            ),
            createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = entryTime,
                updatedAt = parkedTime
            ),
            createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime
            )
        )

        every { spotRepository.findByCoordinates(testLatitude, testLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns vehicleEvents

        val result = spotService.getSpotStatus(testLatitude, testLongitude)

        assertFalse(result.occupied)
        assertEquals(entryTime, result.entryTime)
        assertEquals(exitTime, result.timeParked)
    }

    @Test
    @DisplayName("Should return unoccupied spot when last event is ENTERED")
    fun `should return unoccupied spot when last event is entered`() {
        val spot = createTestSpot()
        val entryTime = LocalDateTime.now().minusMinutes(30)

        val vehicleEvents = listOf(
            createVehicleEvent(VehicleStatus.ENTERED, entryTime, entryTime)
        )

        every { spotRepository.findByCoordinates(testLatitude, testLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns vehicleEvents

        val result = spotService.getSpotStatus(testLatitude, testLongitude)

        assertFalse(result.occupied)
        assertEquals(entryTime, result.entryTime)
        assertEquals(entryTime, result.timeParked)
    }

    @Test
    @DisplayName("Should return unoccupied spot when no vehicle events exist")
    fun `should return unoccupied spot when no vehicle events exist`() {
        val spot = createTestSpot()
        val emptyEvents = emptyList<VehicleEvent>()

        every { spotRepository.findByCoordinates(testLatitude, testLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns emptyEvents

        val result = spotService.getSpotStatus(testLatitude, testLongitude)

        assertFalse(result.occupied)
        assertNull(result.entryTime)
        assertNull(result.timeParked)
    }

    @Test
    @DisplayName("Should handle multiple vehicles in same spot correctly")
    fun `should handle multiple vehicles in same spot correctly`() {
        val spot = createTestSpot()
        val now = LocalDateTime.now()

        val vehicleEvents = listOf(
            // First vehicle
            createVehicleEvent(
                eventType = VehicleStatus.ENTERED,
                entryAt = now.minusHours(3),
                updatedAt = now.minusHours(3),
                licensePlate = "CAR001"
            ),
            createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = now.minusHours(3),
                updatedAt = now.minusHours(2).minusMinutes(30),
                licensePlate = "CAR001"
            ),
            createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = now.minusHours(3),
                updatedAt = now.minusHours(2),
                licensePlate = "CAR001"
            ),
            // Second vehicle (current)
            createVehicleEvent(
                eventType = VehicleStatus.ENTERED,
                entryAt = now.minusHours(1),
                updatedAt = now.minusHours(1),
                licensePlate = "CAR002"
            ),
            createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = now.minusHours(1),
                updatedAt = now.minusMinutes(30),
                licensePlate = "CAR002"
            )
        )

        every { spotRepository.findByCoordinates(testLatitude, testLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns vehicleEvents

        val result = spotService.getSpotStatus(testLatitude, testLongitude)

        assertTrue(result.occupied)
        assertEquals(now.minusHours(1), result.entryTime)
        assertEquals(now.minusMinutes(30), result.timeParked)
    }

    @Test
    @DisplayName("Should handle exact coordinate matching")
    fun `should handle exact coordinate matching`() {
        val spot = createTestSpot()
        val vehicleEvents = listOf(
            createVehicleEvent(
                VehicleStatus.PARKED,
                LocalDateTime.now().minusMinutes(30),
                LocalDateTime.now().minusMinutes(15)
            )
        )

        every { spotRepository.findByCoordinates(testLatitude, testLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns vehicleEvents

        val result = spotService.getSpotStatus(testLatitude, testLongitude)

        assertTrue(result.occupied)
        verify { spotRepository.findByCoordinates(testLatitude, testLongitude) }
    }

    @Test
    @DisplayName("Should propagate exception when spot is not found")
    fun `should propagate exception when spot is not found`() {
        every {
            spotRepository.findByCoordinates(
                testLatitude,
                testLongitude
            )
        } throws RuntimeException("Spot not found")

        assertThrows<RuntimeException>("Spot not found") {
            spotService.getSpotStatus(testLatitude, testLongitude)
        }
    }

    @Test
    @DisplayName("Should handle different coordinate precision")
    fun `should handle different coordinate precision`() {
        val preciseLatitude = -23.5616841234
        val preciseLongitude = -46.6559815678
        val spot = createTestSpot()
        val vehicleEvents = listOf(
            createVehicleEvent(
                VehicleStatus.PARKED,
                LocalDateTime.now().minusMinutes(45),
                LocalDateTime.now().minusMinutes(20)
            )
        )

        every { spotRepository.findByCoordinates(preciseLatitude, preciseLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns vehicleEvents

        val result = spotService.getSpotStatus(preciseLatitude, preciseLongitude)

        assertTrue(result.occupied)
        verify { spotRepository.findByCoordinates(preciseLatitude, preciseLongitude) }
    }

    @Test
    @DisplayName("Should handle edge case with single EXITED event")
    fun `should handle edge case with single exited event`() {
        val spot = createTestSpot()
        val exitTime = LocalDateTime.now().minusMinutes(10)

        val vehicleEvents = listOf(
            createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = exitTime.minusHours(1),
                updatedAt = exitTime
            )
        )

        every { spotRepository.findByCoordinates(testLatitude, testLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns vehicleEvents

        val result = spotService.getSpotStatus(testLatitude, testLongitude)

        assertFalse(result.occupied)
        assertEquals(exitTime.minusHours(1), result.entryTime)
        assertEquals(exitTime, result.timeParked)
    }

    @Test
    @DisplayName("Should handle chronological order of events correctly")
    fun `should handle chronological order of events correctly`() {
        val spot = createTestSpot()
        val baseTime = LocalDateTime.now().minusHours(2)

        // Events in non-chronological order in the list
        val vehicleEvents = listOf(
            createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = baseTime,
                updatedAt = baseTime.plusMinutes(90)
            ),
            createVehicleEvent(
                eventType = VehicleStatus.ENTERED,
                entryAt = baseTime,
                updatedAt = baseTime
            ),
            createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = baseTime,
                updatedAt = baseTime.plusMinutes(30)
            )
        )

        every { spotRepository.findByCoordinates(testLatitude, testLongitude) } returns spot
        every { vehicleEventRepository.getBySpotId(testSpotId) } returns vehicleEvents

        val result = spotService.getSpotStatus(testLatitude, testLongitude)

        // Should use the last event in the list (PARKED), not chronologically last
        assertTrue(result.occupied)
    }

    private fun createTestSpot(
        id: Long = testSpotId,
        sectorId: Long = testSectorId,
        latitude: Double = testLatitude,
        longitude: Double = testLongitude,
        isOccupied: Boolean = false
    ) = Spot(
        id = id,
        sectorId = sectorId,
        latitude = latitude,
        longitude = longitude,
        isOccupied = isOccupied
    )

    private fun createVehicleEvent(
        eventType: VehicleStatus,
        entryAt: LocalDateTime,
        updatedAt: LocalDateTime,
        licensePlate: String = testLicensePlate,
        spotId: Long = testSpotId
    ) = VehicleEvent(
        id = System.currentTimeMillis(),
        licensePlate = licensePlate,
        eventType = eventType,
        latitude = testLatitude,
        longitude = testLongitude,
        spotId = spotId,
        sectorId = testSectorId,
        entryAt = entryAt,
        updatedAt = updatedAt
    )
}

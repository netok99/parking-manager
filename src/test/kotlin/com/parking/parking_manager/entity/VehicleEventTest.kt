package com.parking.parking_manager.entity

import arrow.core.None
import arrow.core.Some
import com.parking.parking_manager.domain.entity.Sector
import com.parking.parking_manager.domain.entity.VehicleEvent
import com.parking.parking_manager.domain.entity.VehicleStatus
import com.parking.parking_manager.domain.entity.calculateAmountFromVehicleEventExited
import com.parking.parking_manager.domain.entity.calculateCurrentPrice
import com.parking.parking_manager.domain.entity.filteredStatusEntered
import com.parking.parking_manager.domain.entity.filteredStatusParked
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class VehicleEventTest {

    @Nested
    @DisplayName("VehicleEvents Extension Functions")
    inner class VehicleEventsExtensionTests {

        @Test
        @DisplayName("Should return last ENTERED event when filteredStatusEntered is called")
        fun `should return last entered event`() {
            val enteredEvent1 = createVehicleEvent(
                eventType = VehicleStatus.ENTERED,
                entryAt = LocalDateTime.now().minusHours(2)
            )
            val parkedEvent = createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = LocalDateTime.now().minusHours(1)
            )
            val enteredEvent2 = createVehicleEvent(
                eventType = VehicleStatus.ENTERED,
                entryAt = LocalDateTime.now()
            )
            val events = listOf(enteredEvent1, parkedEvent, enteredEvent2)

            val result = events.filteredStatusEntered()

            assertTrue(result is Some)
        }

        @Test
        @DisplayName("Should return None when no ENTERED event exists")
        fun `should return none when no entered event exists`() {
            val parkedEvent = createVehicleEvent(eventType = VehicleStatus.PARKED)
            val exitedEvent = createVehicleEvent(eventType = VehicleStatus.EXITED)
            val events = listOf(parkedEvent, exitedEvent)

            val result = events.filteredStatusEntered()

            assertTrue(result is None)
        }

        @Test
        @DisplayName("Should return last PARKED event when filteredStatusParked is called")
        fun `should return last parked event`() {
            val enteredEvent =
                createVehicleEvent(eventType = VehicleStatus.ENTERED, entryAt = LocalDateTime.now().minusHours(2))
            val parkedEvent1 =
                createVehicleEvent(eventType = VehicleStatus.PARKED, entryAt = LocalDateTime.now().minusHours(1))
            val parkedEvent2 = createVehicleEvent(eventType = VehicleStatus.PARKED, entryAt = LocalDateTime.now())
            val events = listOf(enteredEvent, parkedEvent1, parkedEvent2)

            val result = events.filteredStatusParked()

            assertTrue(result is Some)
        }

        @Test
        @DisplayName("Should return None when no PARKED event exists")
        fun `should return none when no parked event exists`() {
            val enteredEvent = createVehicleEvent(eventType = VehicleStatus.ENTERED)
            val exitedEvent = createVehicleEvent(eventType = VehicleStatus.EXITED)
            val events = listOf(enteredEvent, exitedEvent)

            val result = events.filteredStatusParked()

            assertTrue(result is None)
        }

        @Test
        @DisplayName("Should return None when events list is empty")
        fun `should return none when events list is empty`() {
            val events = emptyList<VehicleEvent>()

            val enteredResult = events.filteredStatusEntered()
            val parkedResult = events.filteredStatusParked()

            assertTrue(enteredResult is None)
            assertTrue(parkedResult is None)
        }
    }

    @Nested
    @DisplayName("Calculate Amount From Vehicle Event Exited")
    inner class CalculateAmountTests {

        private val sector = createSector()

        @Test
        @DisplayName("Should return zero when event type is not EXITED")
        fun `should return zero when event type is not exited`() {
            val enteredEvent = createVehicleEvent(eventType = VehicleStatus.ENTERED)
            val parkedEvent = createVehicleEvent(eventType = VehicleStatus.PARKED)

            val resultEntered = calculateAmountFromVehicleEventExited(enteredEvent, sector)
            val resultParked = calculateAmountFromVehicleEventExited(parkedEvent, sector)

            assertEquals(BigDecimal.ZERO, resultEntered)
            assertEquals(BigDecimal.ZERO, resultParked)
        }

        @Test
        @DisplayName("Should return zero for grace period (15 minutes)")
        fun `should return zero for grace period`() {
            val entryTime = LocalDateTime.now()
            val exitTime = entryTime.plusMinutes(10) // Within grace period
            val exitEvent = createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00")
            )

            val result = calculateAmountFromVehicleEventExited(exitEvent, sector)

            assertEquals(BigDecimal.ZERO, result)
        }

        @Test
        @DisplayName("Should charge full price for first hour after grace period")
        fun `should charge full price for first hour after grace period`() {
            val entryTime = LocalDateTime.now()
            val exitTime = entryTime.plusMinutes(30) // 30 minutes = 15 minutes chargeable
            val exitEvent = createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal.ZERO
            )

            val result = calculateAmountFromVehicleEventExited(exitEvent, sector)

            assertEquals(BigDecimal("10.00"), result)
        }

        @Test
        @DisplayName("Should apply negative dynamic rate (discount)")
        fun `should apply negative dynamic rate discount`() {
            val entryTime = LocalDateTime.now()
            val exitTime = entryTime.plusMinutes(30)
            val exitEvent = createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal("-10") // 10% discount
            )

            val result = calculateAmountFromVehicleEventExited(exitEvent, sector)

            assertEquals(BigDecimal("9.00"), result)
        }

        @Test
        @DisplayName("Should apply positive dynamic rate (increase)")
        fun `should apply positive dynamic rate increase`() {
            val entryTime = LocalDateTime.now()
            val exitTime = entryTime.plusMinutes(30)
            val exitEvent = createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal("25") // 25% increase
            )

            val result = calculateAmountFromVehicleEventExited(exitEvent, sector)

            assertEquals(BigDecimal("12.50"), result)
        }

        @Test
        @DisplayName("Should calculate pro-rata billing for additional periods")
        fun `should calculate pro rata billing for additional periods`() {
            val entryTime = LocalDateTime.now()
            val exitTime = entryTime.plusMinutes(90) // 1h30min = 75 minutes chargeable
            val exitEvent = createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal.ZERO
            )

            val result = calculateAmountFromVehicleEventExited(exitEvent, sector)

            // First hour + 15 minutes (1 additional 15-min period = 0.25 hours)
            // Total: 1.25 hours * 10.00 = 12.50
            assertEquals(BigDecimal("12.50"), result)
        }

        @Test
        @DisplayName("Should respect sector duration limit")
        fun `should respect sector duration limit`() {
            val shortLimitSector = createSector(durationLimitMinutes = 60) // 1 hour limit
            val entryTime = LocalDateTime.now()
            val exitTime = entryTime.plusMinutes(120) // 2 hours parked
            val exitEvent = createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal.ZERO
            )

            val result = calculateAmountFromVehicleEventExited(exitEvent, shortLimitSector)

            // Should only charge for 60 minutes (45 minutes chargeable after grace period)
            assertEquals(BigDecimal("10.00"), result)
        }
    }

    @Nested
    @DisplayName("Calculate Current Price")
    inner class CalculateCurrentPriceTests {

        private val sector = createSector()

        @Test
        @DisplayName("Should return zero for empty vehicle events")
        fun `should return zero for empty vehicle events`() {
            val events = emptyList<VehicleEvent>()
            val currentTime = LocalDateTime.now()

            val result = calculateCurrentPrice(events, currentTime, sector)

            assertEquals(BigDecimal.ZERO, result)
        }

        @Test
        @DisplayName("Should return zero when last event is not PARKED")
        fun `should return zero when last event is not parked`() {
            val events = listOf(
                createVehicleEvent(eventType = VehicleStatus.ENTERED),
                createVehicleEvent(eventType = VehicleStatus.EXITED)
            )
            val currentTime = LocalDateTime.now()

            val result = calculateCurrentPrice(events, currentTime, sector)

            assertEquals(BigDecimal.ZERO, result)
        }

        @Test
        @DisplayName("Should return zero during grace period")
        fun `should return zero during grace period`() {
            val entryTime = LocalDateTime.now().minusMinutes(10)
            val parkedEvent = createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = entryTime,
                sectorBasePrice = BigDecimal("10.00")
            )
            val events = listOf(parkedEvent)
            val currentTime = LocalDateTime.now()

            val result = calculateCurrentPrice(events, currentTime, sector)

            assertEquals(BigDecimal.ZERO, result)
        }

        @Test
        @DisplayName("Should calculate current price with dynamic rate")
        fun `should calculate current price with dynamic rate`() {
            val entryTime = LocalDateTime.now().minusMinutes(30)
            val parkedEvent = createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = entryTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal("10") // 10% increase
            )
            val events = listOf(parkedEvent)
            val currentTime = LocalDateTime.now()

            val result = calculateCurrentPrice(events, currentTime, sector)

            assertEquals(BigDecimal("11.00"), result)
        }

        @Test
        @DisplayName("Should handle null sector")
        fun `should handle null sector`() {
            val entryTime = LocalDateTime.now().minusMinutes(30)
            val parkedEvent = createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = entryTime,
                sectorBasePrice = BigDecimal("10.00")
            )
            val events = listOf(parkedEvent)
            val currentTime = LocalDateTime.now()

            val result = calculateCurrentPrice(events, currentTime, null)

            assertEquals(BigDecimal("10.00"), result)
        }

        @Test
        @DisplayName("Should apply sector duration limit to current price calculation")
        fun `should apply sector duration limit to current price calculation`() {
            val shortLimitSector = createSector(durationLimitMinutes = 60)
            val entryTime = LocalDateTime.now().minusMinutes(120) // 2 hours ago
            val parkedEvent = createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = entryTime,
                sectorBasePrice = BigDecimal("10.00")
            )
            val events = listOf(parkedEvent)
            val currentTime = LocalDateTime.now()

            val result = calculateCurrentPrice(events, currentTime, shortLimitSector)

            // Should only calculate for 60 minutes (45 chargeable after grace period)
            assertEquals(BigDecimal("10.00"), result)
        }
    }

    @Nested
    @DisplayName("Complex Scenarios")
    inner class ComplexScenariosTests {

        @Test
        @DisplayName("Should handle multiple events with different dynamic rates")
        fun `should handle multiple events with different dynamic rates`() {
            val enteredEvent = createVehicleEvent(
                eventType = VehicleStatus.ENTERED,
                entryAt = LocalDateTime.now().minusHours(1),
                dynamicRate = BigDecimal("-10")
            )
            val parkedEvent = createVehicleEvent(
                eventType = VehicleStatus.PARKED,
                entryAt = LocalDateTime.now().minusMinutes(30),
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal("25") // Different rate for parked event
            )
            val events = listOf(enteredEvent, parkedEvent)
            val currentTime = LocalDateTime.now()
            val sector = createSector()

            val result = calculateCurrentPrice(events, currentTime, sector)

            // Should use the PARKED event's dynamic rate (25% increase)
            assertEquals(BigDecimal("12.50"), result)
        }

        @Test
        @DisplayName("Should handle edge case with exact 15-minute periods")
        fun `should handle edge case with exact 15 minute periods`() {
            val entryTime = LocalDateTime.now()
            val exitTime = entryTime.plusMinutes(105) // 1h45min = 90 minutes chargeable
            val exitEvent = createVehicleEvent(
                eventType = VehicleStatus.EXITED,
                entryAt = entryTime,
                updatedAt = exitTime,
                sectorBasePrice = BigDecimal("10.00"),
                dynamicRate = BigDecimal.ZERO
            )
            val sector = createSector()

            val result = calculateAmountFromVehicleEventExited(exitEvent, sector)

            // First hour + 30 minutes (2 additional 15-min periods = 0.5 hours)
            // Total: 1.5 hours * 10.00 = 15.00
            assertEquals(BigDecimal("15.00"), result)
        }
    }

    private fun createVehicleEvent(
        id: Long? = null,
        licensePlate: String = "TEST123",
        eventType: VehicleStatus = VehicleStatus.ENTERED,
        latitude: Double? = null,
        longitude: Double? = null,
        spotId: Long? = null,
        sectorId: Long? = null,
        sectorBasePrice: BigDecimal? = null,
        dynamicRate: BigDecimal = BigDecimal.ZERO,
        entryAt: LocalDateTime = LocalDateTime.now(),
        createdAt: LocalDateTime = LocalDateTime.now(),
        updatedAt: LocalDateTime = LocalDateTime.now()
    ) = VehicleEvent(
        id = id,
        licensePlate = licensePlate,
        eventType = eventType,
        latitude = latitude,
        longitude = longitude,
        spotId = spotId,
        sectorId = sectorId,
        sectorBasePrice = sectorBasePrice,
        dynamicRate = dynamicRate,
        entryAt = entryAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun createSector(
        id: Long = 1L,
        sectorCode: String = "A",
        basePrice: BigDecimal = BigDecimal("10.00"),
        maxCapacity: Int = 100,
        openHour: LocalTime = LocalTime.of(8, 0),
        closeHour: LocalTime = LocalTime.of(22, 0),
        durationLimitMinutes: Int = 240,
        currencyCode: String = "BRL"
    ) = Sector(
        id = id,
        sectorCode = sectorCode,
        basePrice = basePrice,
        maxCapacity = maxCapacity,
        openHour = openHour,
        closeHour = closeHour,
        durationLimitMinutes = durationLimitMinutes,
        currencyCode = currencyCode
    )
}

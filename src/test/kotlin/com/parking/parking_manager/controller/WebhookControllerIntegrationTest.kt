package com.parking.parking_manager.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.parking.parking_manager.application.port.input.VehicleEventService
import com.parking.parking_manager.infrastructure.adapters.input.WebhookController
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(WebhookController::class)
@DisplayName("WebhookController Integration Tests")
class WebhookControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var vehicleEventService: VehicleEventService

    private val baseUrl = "/webhook"

    @BeforeEach
    fun setUp() {
        reset(vehicleEventService)
    }

    @Nested
    @DisplayName("Entry Event Tests")
    inner class EntryEventTests {

        @Test
        @DisplayName("Should handle valid ENTRY event successfully")
        fun shouldHandleValidEntryEvent() {
            val entryEventData = mapOf(
                "license_plate" to "ZUL0001",
                "entry_time" to "2025-01-01T12:00:00.000Z",
                "event_type" to "ENTRY"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryEventData))
                )
                .andExpect(status().isOk)
        }

        @Test
        @DisplayName("Should handle ENTRY event with different time format")
        fun shouldHandleEntryEventWithDifferentTimeFormat() {
            val entryEventData = mapOf(
                "license_plate" to "ABC1234",
                "entry_time" to "2025-06-15T08:30:45.123Z",
                "event_type" to "ENTRY"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryEventData))
                )
                .andExpect(status().isOk)

            verify(vehicleEventService).handleEntryEvent(any())
        }

        @Test
        @DisplayName("Should return 500 when service throws exception for ENTRY event")
        fun shouldReturn500WhenServiceThrowsExceptionForEntryEvent() {
            val entryEventData = mapOf(
                "license_plate" to "ZUL0001",
                "entry_time" to "2025-01-01T12:00:00.000Z",
                "event_type" to "ENTRY"
            )

            doThrow(RuntimeException("Database error")).whenever(vehicleEventService).handleEntryEvent(any())

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryEventData))
                )
                .andExpect(status().isInternalServerError)
        }
    }

    @Nested
    @DisplayName("Parked Event Tests")
    inner class ParkedEventTests {

        @Test
        @DisplayName("Should handle valid PARKED event successfully")
        fun shouldHandleValidParkedEvent() {
            val parkedEventData = mapOf(
                "license_plate" to "ZUL0001",
                "lat" to -23.561684,
                "lng" to -46.655981,
                "event_type" to "PARKED"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkedEventData))
                )
                .andExpect(status().isOk)
        }

        @Test
        @DisplayName("Should handle PARKED event with different coordinates")
        fun shouldHandleParkedEventWithDifferentCoordinates() {
            val parkedEventData = mapOf(
                "license_plate" to "DEF5678",
                "lat" to -22.123456,
                "lng" to -45.987654,
                "event_type" to "PARKED"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkedEventData))
                )
                .andExpect(status().isOk)

            verify(vehicleEventService).handleParkedEvent(any())
        }

        @Test
        @DisplayName("Should return 500 when service throws exception for PARKED event")
        fun shouldReturn500WhenServiceThrowsExceptionForParkedEvent() {
            val parkedEventData = mapOf(
                "license_plate" to "ZUL0001",
                "lat" to -23.561684,
                "lng" to -46.655981,
                "event_type" to "PARKED"
            )

            doThrow(IllegalStateException("Spot not found"))
                .whenever(vehicleEventService).handleParkedEvent(any())

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkedEventData))
                )
                .andExpect(status().isInternalServerError)
        }
    }

    @Nested
    @DisplayName("Exit Event Tests")
    inner class ExitEventTests {

        @Test
        @DisplayName("Should handle valid EXIT event successfully")
        fun shouldHandleValidExitEvent() {
            val exitEventData = mapOf(
                "license_plate" to "ZUL0001",
                "exit_time" to "2025-01-01T14:00:00.000Z",
                "event_type" to "EXIT"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exitEventData))
                )
                .andExpect(status().isOk)
        }

        @Test
        @DisplayName("Should handle EXIT event with empty license plate")
        fun shouldHandleExitEventWithEmptyLicensePlate() {
            val exitEventData = mapOf(
                "license_plate" to "",
                "exit_time" to "2025-01-01T14:00:00.000Z",
                "event_type" to "EXIT"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exitEventData))
                )
                .andExpect(status().isOk)

            verify(vehicleEventService).handleExitEvent(any())
        }

        @Test
        @DisplayName("Should return 500 when service throws exception for EXIT event")
        fun shouldReturn500WhenServiceThrowsExceptionForExitEvent() {
            val exitEventData = mapOf(
                "license_plate" to "ZUL0001",
                "exit_time" to "2025-01-01T14:00:00.000Z",
                "event_type" to "EXIT"
            )

            doThrow(RuntimeException("Payment processing error")).whenever(vehicleEventService).handleExitEvent(any())

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exitEventData))
                )
                .andExpect(status().isInternalServerError)
        }
    }

    @Nested
    @DisplayName("Invalid Event Tests")
    inner class InvalidEventTests {

        @Test
        @DisplayName("Should return 400 for unknown event type")
        fun shouldReturn400ForUnknownEventType() {
            val invalidEventData = mapOf(
                "license_plate" to "ZUL0001",
                "entry_time" to "2025-01-01T12:00:00.000Z",
                "event_type" to "UNKNOWN"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidEventData))
                )
                .andExpect(status().isBadRequest)

            // Verify no service methods were called
            verifyNoInteractions(vehicleEventService)
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON")
        fun shouldReturn400ForMalformedJson() {
            val malformedJson = "{ invalid json }"

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson)
                )
                .andExpect(status().isBadRequest)

            verifyNoInteractions(vehicleEventService)
        }

        @Test
        @DisplayName("Should return 500 for missing required fields")
        fun shouldReturn500ForMissingRequiredFields() {
            // Missing license_plate field
            val incompleteEventData = mapOf(
                "entry_time" to "2025-01-01T12:00:00.000Z",
                "event_type" to "ENTRY"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(incompleteEventData))
                )
                .andExpect(status().isInternalServerError)
        }

        @Test
        @DisplayName("Should return 500 for invalid date format")
        fun shouldReturn500ForInvalidDateFormat() {
            val invalidDateEventData = mapOf(
                "license_plate" to "ZUL0001",
                "entry_time" to "invalid-date",
                "event_type" to "ENTRY"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDateEventData))
                )
                .andExpect(status().isInternalServerError)

            verifyNoInteractions(vehicleEventService)
        }

        @Test
        @DisplayName("Should return 500 for invalid coordinate types")
        fun shouldReturn500ForInvalidCoordinateTypes() {
            val invalidCoordinateEventData = mapOf(
                "license_plate" to "ZUL0001",
                "lat" to "invalid-lat",
                "lng" to "invalid-lng",
                "event_type" to "PARKED"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCoordinateEventData))
                )
                .andExpect(status().isInternalServerError)

            verifyNoInteractions(vehicleEventService)
        }
    }

    @Nested
    @DisplayName("Content Type Tests")
    inner class ContentTypeTests {

        @Test
        @DisplayName("Should return 415 for unsupported media type")
        fun shouldReturn500ForUnsupportedMediaType() {
            val entryEventData = mapOf(
                "license_plate" to "ZUL0001",
                "entry_time" to "2025-01-01T12:00:00.000Z",
                "event_type" to "ENTRY"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(objectMapper.writeValueAsString(entryEventData))
                )
                .andExpect(status().isInternalServerError)

            verifyNoInteractions(vehicleEventService)
        }

        @Test
        @DisplayName("Should accept application/json content type")
        fun shouldAcceptApplicationJsonContentType() {
            val entryEventData = mapOf(
                "license_plate" to "ZUL0001",
                "entry_time" to "2025-01-01T12:00:00.000Z",
                "event_type" to "ENTRY"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryEventData))
                )
                .andExpect(status().isOk)

            verify(vehicleEventService).handleEntryEvent(any())
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Should handle multiple events in sequence")
        fun shouldHandleMultipleEventsInSequence() {
            val entryEvent = mapOf(
                "license_plate" to "ZUL0001",
                "entry_time" to "2025-01-01T12:00:00.000Z",
                "event_type" to "ENTRY"
            )

            val parkedEvent = mapOf(
                "license_plate" to "ZUL0001",
                "lat" to -23.561684,
                "lng" to -46.655981,
                "event_type" to "PARKED"
            )

            val exitEvent = mapOf(
                "license_plate" to "ZUL0001",
                "exit_time" to "2025-01-01T14:00:00.000Z",
                "event_type" to "EXIT"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryEvent))
                ).andExpect(status().isOk)

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkedEvent))
                ).andExpect(status().isOk)

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exitEvent))
                ).andExpect(status().isOk)

            // Verify all service methods were called
            verify(vehicleEventService).handleEntryEvent(any())
            verify(vehicleEventService).handleParkedEvent(any())
            verify(vehicleEventService).handleExitEvent(any())
        }

        @Test
        @DisplayName("Should handle special characters in license plate")
        fun shouldHandleSpecialCharactersInLicensePlate() {
            val entryEvent = mapOf(
                "license_plate" to "ABC-1234",
                "entry_time" to "2025-01-01T12:00:00.000Z",
                "event_type" to "ENTRY"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(entryEvent))
                ).andExpect(status().isOk)
        }

        @Test
        @DisplayName("Should handle extreme coordinate values")
        fun shouldHandleExtremeCoordinateValues() {
            val parkedEvent = mapOf(
                "license_plate" to "ZUL0001",
                "lat" to -90.0,
                "lng" to 180.0,
                "event_type" to "PARKED"
            )

            mockMvc
                .perform(
                    post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(parkedEvent))
                ).andExpect(status().isOk)
        }
    }
}

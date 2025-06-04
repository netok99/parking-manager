package com.parking.parking_manager.infrastructure.adapters.input

import com.parking.parking_manager.application.port.input.VehicleEventService
import com.parking.parking_manager.infrastructure.adapters.model.EntryEventRequest
import com.parking.parking_manager.infrastructure.adapters.model.ExitEventRequest
import com.parking.parking_manager.infrastructure.adapters.model.ParkedEventRequest
import java.time.ZonedDateTime
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/webhook")
@Validated
class WebhookController(private val vehicleEventService: VehicleEventService) {

    private val logger = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping
    fun handleVehicleEvent(@RequestBody eventData: Map<String, Any>): ResponseEntity<Void> {
        logger.info("Received webhook event: $eventData")
        try {
            when (eventData["event_type"]) {
                "ENTRY" -> {
                    val entryEvent = mapToEntryEvent(eventData)
                    vehicleEventService.handleEntryEvent(entryEvent)
                }

                "PARKED" -> {
                    val parkedEvent = mapToParkedEvent(eventData)
                    vehicleEventService.handleParkedEvent(parkedEvent)
                }

                "EXIT" -> {
                    val exitEvent = mapToExitEvent(eventData)
                    vehicleEventService.handleExitEvent(exitEvent)
                }

                else -> {
                    logger.warn("Unknown event type: ${eventData["event_type"]}")
                    return ResponseEntity.badRequest().build()
                }
            }
            return ResponseEntity.ok().build()
        } catch (e: Exception) {
            logger.error("Error processing webhook event", e)
            return ResponseEntity.internalServerError().build()
        }
    }

    private fun mapToEntryEvent(data: Map<String, Any>) = EntryEventRequest(
        licensePlate = data["license_plate"] as String,
        entryTime = ZonedDateTime.parse((data["entry_time"] as String)).toLocalDateTime(),
        eventType = data["event_type"] as String
    )

    private fun mapToParkedEvent(data: Map<String, Any>) = ParkedEventRequest(
        licensePlate = data["license_plate"] as String,
        latitude = data["lat"] as Double,
        longitude = data["lng"] as Double,
        eventType = data["event_type"] as String
    )

    private fun mapToExitEvent(data: Map<String, Any>) = ExitEventRequest(
        licensePlate = data["license_plate"] as String,
        exitTime = ZonedDateTime.parse(data["exit_time"] as String).toLocalDateTime(),
        eventType = data["event_type"] as String
    )
}

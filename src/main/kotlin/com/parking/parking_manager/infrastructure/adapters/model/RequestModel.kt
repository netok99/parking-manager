package com.parking.parking_manager.infrastructure.adapters.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDateTime

data class CreateGarageRequest(
    @NotEmpty(message = "Garage list cannot be empty.")
    @field:Valid
    val garage: List<@Valid GarageRequest>,

    @NotEmpty(message = "Spot list cannot be empty.")
    @field:Valid
    val spots: List<@Valid SpotsRequest>
)

data class GarageRequest(
    @field:NotBlank(message = "Sector is required")
    val sector: String,

    @field:NotNull(message = "Latitude is required")
    @JsonProperty("basePrice")
    val basePrice: BigDecimal,

    @field:NotNull(message = "Max capacity is required")
    @JsonProperty("max_capacity")
    val maxCapacity: Int,

    @field:NotBlank(message = "Open hour is required")
    @field:Pattern(
        regexp = "^\\d{2}:\\d{2}$",
        message = "Open hour must be in format HH-MN"
    )
    @JsonProperty("open_hour")
    val openHour: String,

    @field:NotBlank(message = "Close hour is required")
    @field:Pattern(
        regexp = "^\\d{2}:\\d{2}$",
        message = "Close hour must be in format HH-MN"
    )
    @JsonProperty("close_hour")
    val closeHour: String,

    @field:NotNull(message = "Duration limit in minutes is required")
    @JsonProperty("duration_limit_minutes")
    val durationLimitMinutes: Int
)

data class SpotsRequest(
    @field:NotNull(message = "Spot id is required")
    val id: Int,

    @field:NotBlank(message = "Close hour is required")
    val sector: String,

    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val lat: Double,

    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val lng: Double
)

data class PlateStatusRequest(
    @field:NotBlank(message = "License plate is required")
    @field:Pattern(
        regexp = "^[A-Z]{3}[0-9]{4}$|^[A-Z]{3}[0-9][A-Z][0-9]{2}$",
        message = "Invalid license plate format"
    )
    @JsonProperty("license_plate")
    val licensePlate: String
)

data class SpotStatusRequest(
    @field:NotNull(message = "Latitude is required")
    @field:DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @field:DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    val lat: Double,

    @field:NotNull(message = "Longitude is required")
    @field:DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @field:DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    val lng: Double
)

data class RevenueRequest(
    @field:NotNull(message = "Date is required")
    @field:Pattern(
        regexp = "^\\d{4}-\\d{2}-\\d{2}$",
        message = "Date must be in format YYYY-MM-DD"
    )
    val date: String,

    @field:NotBlank(message = "Sector is required")
    @field:Size(min = 1, max = 2, message = "Sector must be between 1 and 2 characters")
    val sector: String
)

data class EntryEventRequest(
    @field:NotBlank(message = "License plate is required")
    @JsonProperty("license_plate")
    val licensePlate: String,

    @field:NotNull(message = "Entry time is required")
    @JsonProperty("entry_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val entryTime: LocalDateTime,

    @field:NotBlank(message = "Event type is required")
    @field:Pattern(regexp = "ENTRY", message = "Event type must be ENTRY")
    @JsonProperty("event_type")
    val eventType: String
)

data class ParkedEventRequest(
    @field:NotBlank(message = "License plate is required")
    @JsonProperty("license_plate")
    val licensePlate: String,

    @field:NotNull(message = "Latitude is required")
    val latitude: Double,

    @field:NotNull(message = "Longitude is required")
    val longitude: Double,

    @field:NotBlank(message = "Event type is required")
    @field:Pattern(regexp = "PARKED", message = "Event type must be PARKED")
    @JsonProperty("event_type")
    val eventType: String
)

data class ExitEventRequest(
    @field:NotBlank(message = "License plate is required")
    @JsonProperty("license_plate")
    val licensePlate: String,

    @field:NotNull(message = "Exit time is required")
    @JsonProperty("exit_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val exitTime: LocalDateTime,

    @field:NotBlank(message = "Event type is required")
    @field:Pattern(regexp = "EXIT", message = "Event type must be EXIT")
    @JsonProperty("event_type")
    val eventType: String
)

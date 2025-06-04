package com.parking.parking_manager.infrastructure.adapters.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import java.math.BigDecimal
import java.time.LocalDateTime

data class PlateStatusResponse(
    @JsonProperty("license_plate")
    val licensePlate: String,
    @JsonProperty("price_until_now")
    val priceUntilNow: BigDecimal,
    @JsonProperty("entry_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val entryTime: LocalDateTime?,
    @JsonProperty("time_parked")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val timeParked: LocalDateTime?
)

data class SpotStatusResponse(
    val occupied: Boolean,
    @JsonProperty("entry_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val entryTime: LocalDateTime?,
    @JsonProperty("time_parked")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val timeParked: LocalDateTime?
)

data class RevenueResponse(
    val amount: BigDecimal,
    val currency: String = "BRL",
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ApiErrorResponse(
    val message: String,
    val errors: List<String>? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

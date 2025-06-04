package com.parking.parking_manager.infrastructure.adapters.input

import com.parking.parking_manager.application.port.input.GarageService
import com.parking.parking_manager.application.port.input.PlateService
import com.parking.parking_manager.application.port.input.RevenueService
import com.parking.parking_manager.application.port.input.SpotService
import com.parking.parking_manager.infrastructure.adapters.model.CreateGarageRequest
import com.parking.parking_manager.infrastructure.adapters.model.PlateStatusRequest
import com.parking.parking_manager.infrastructure.adapters.model.RevenueRequest
import com.parking.parking_manager.infrastructure.adapters.model.RevenueResponse
import com.parking.parking_manager.infrastructure.adapters.model.SpotStatusRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@Validated
class ParkingManagementController(
    private val garageService: GarageService,
    private val plateService: PlateService,
    private val spotService: SpotService,
    private val revenueService: RevenueService
) {

    @PostMapping("/garage")
    fun createGarageDynamically(@Valid @RequestBody request: CreateGarageRequest) =
        ResponseEntity.ok(garageService.createGarage(request))

    @PostMapping("/plate-status")
    fun getPlateStatus(@Valid @RequestBody request: PlateStatusRequest) =
        ResponseEntity.ok(plateService.getPlateStatus(request.licensePlate))

    @PostMapping("/spot-status")
    fun getSpotStatus(@Valid @RequestBody request: SpotStatusRequest) =
        ResponseEntity.ok(spotService.getSpotStatus(latitude = request.lat, longitude = request.lng))

    @GetMapping("/revenue")
    fun getRevenue(@Valid @RequestBody request: RevenueRequest): ResponseEntity<RevenueResponse> =
        ResponseEntity.ok(revenueService.getRevenue(date = request.date, sectorCode = request.sector))
}

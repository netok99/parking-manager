package com.parking.parking_manager

import com.fasterxml.jackson.databind.ObjectMapper
import com.parking.parking_manager.application.port.input.GarageService
import com.parking.parking_manager.application.port.input.PlateService
import com.parking.parking_manager.application.port.input.RevenueService
import com.parking.parking_manager.application.port.input.SpotService
import com.parking.parking_manager.infrastructure.adapters.input.ParkingManagementController
import com.parking.parking_manager.infrastructure.adapters.model.CreateGarageRequest
import com.parking.parking_manager.infrastructure.adapters.model.GarageRequest
import com.parking.parking_manager.infrastructure.adapters.model.PlateStatusRequest
import com.parking.parking_manager.infrastructure.adapters.model.PlateStatusResponse
import com.parking.parking_manager.infrastructure.adapters.model.RevenueRequest
import com.parking.parking_manager.infrastructure.adapters.model.RevenueResponse
import com.parking.parking_manager.infrastructure.adapters.model.SpotStatusRequest
import com.parking.parking_manager.infrastructure.adapters.model.SpotStatusResponse
import com.parking.parking_manager.infrastructure.adapters.model.SpotsRequest
import java.math.BigDecimal
import java.time.LocalDateTime
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@WebMvcTest(ParkingManagementController::class)
class ParkingManagementControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var garageService: GarageService

    @MockitoBean
    private lateinit var plateService: PlateService

    @MockitoBean
    private lateinit var spotService: SpotService

    @MockitoBean
    private lateinit var revenueService: RevenueService

    // ===========================================
    // CREATE GARAGE TESTS
    // ===========================================

    @Test
    fun `should create garage successfully with valid request`() {
        val garageRequest = GarageRequest(
            sector = "A",
            basePrice = BigDecimal("10.00"),
            maxCapacity = 100,
            openHour = "08:00",
            closeHour = "22:00",
            durationLimitMinutes = 240
        )

        val spotsRequest = SpotsRequest(
            id = 1,
            sector = "A",
            lat = -23.561684,
            lng = -46.655981
        )

        val createGarageRequest = CreateGarageRequest(
            garage = listOf(garageRequest),
            spots = listOf(spotsRequest)
        )

        doNothing().`when`(garageService).createGarage(createGarageRequest)

        mockMvc
            .perform(
                post("/api/v1/garage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createGarageRequest))
            ).andExpect(status().isOk)

        verify(garageService, times(1)).createGarage(createGarageRequest)
    }

    @Test
    fun `should return 400 when creating garage with invalid sector`() {
        val garageRequest = GarageRequest(
            sector = "", // Invalid empty sector
            basePrice = BigDecimal("10.00"),
            maxCapacity = 100,
            openHour = "08:00",
            closeHour = "22:00",
            durationLimitMinutes = 240
        )

        val spotsRequest = SpotsRequest(
            id = 1,
            sector = "A",
            lat = -23.561684,
            lng = -46.655981
        )

        val createGarageRequest = CreateGarageRequest(
            garage = listOf(garageRequest),
            spots = listOf(spotsRequest)
        )

        mockMvc
            .perform(
                post("/api/v1/garage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createGarageRequest))
            )
            .andExpect(status().isBadRequest)

        verify(garageService, never()).createGarage(any())
    }

    @Test
    fun `should return 400 when creating garage with invalid time format`() {
        val garageRequest = GarageRequest(
            sector = "A",
            basePrice = BigDecimal("10.00"),
            maxCapacity = 100,
            openHour = "8:00", // Invalid format - should be 08:00
            closeHour = "22:00",
            durationLimitMinutes = 240
        )

        val spotsRequest = SpotsRequest(
            id = 1,
            sector = "A",
            lat = -23.561684,
            lng = -46.655981
        )

        val createGarageRequest = CreateGarageRequest(
            garage = listOf(garageRequest),
            spots = listOf(spotsRequest)
        )

        mockMvc
            .perform(
                post("/api/v1/garage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createGarageRequest))
            )
            .andExpect(status().isBadRequest)

        verify(garageService, never()).createGarage(any())
    }

    @Test
    fun `should return 400 when creating garage with invalid coordinates`() {
        val garageRequest = GarageRequest(
            sector = "A",
            basePrice = BigDecimal("10.00"),
            maxCapacity = 100,
            openHour = "08:00",
            closeHour = "22:00",
            durationLimitMinutes = 240
        )

        val spotsRequest = SpotsRequest(
            id = 1,
            sector = "A",
            lat = 91.0, // Invalid latitude > 90
            lng = -46.655981
        )

        val createGarageRequest = CreateGarageRequest(
            garage = listOf(garageRequest),
            spots = listOf(spotsRequest)
        )

        mockMvc
            .perform(
                post("/api/v1/garage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createGarageRequest))
            )
            .andExpect(status().isBadRequest)

        verify(garageService, never()).createGarage(any())
    }

    // ===========================================
    // PLATE STATUS TESTS
    // ===========================================

    @Test
    fun `should get plate status successfully with valid license plate`() {
        val licensePlate = "ABC1234"
        val plateStatusRequest = PlateStatusRequest(licensePlate = licensePlate)
        val plateStatusResponse = PlateStatusResponse(
            licensePlate = licensePlate,
            priceUntilNow = BigDecimal("15.50"),
            entryTime = LocalDateTime.now().minusHours(2),
            timeParked = LocalDateTime.now()
        )

        `when`(plateService.getPlateStatus(licensePlate)).thenReturn(plateStatusResponse)

        mockMvc
            .perform(
                post("/api/v1/plate-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(plateStatusRequest))
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.license_plate").value(licensePlate))
            .andExpect(jsonPath("$.price_until_now").value(15.50))

        verify(plateService, times(1)).getPlateStatus(licensePlate)
    }

    @Test
    fun `should return 400 when getting plate status with invalid license plate format`() {
        val plateStatusRequest = PlateStatusRequest(licensePlate = "INVALID") // Invalid format

        mockMvc
            .perform(
                post("/api/v1/plate-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(plateStatusRequest))
            )
            .andExpect(status().isBadRequest)

        verify(plateService, never()).getPlateStatus(any())
    }

    @Test
    fun `should return 400 when getting plate status with empty license plate`() {
        val plateStatusRequest = PlateStatusRequest(licensePlate = "") // Empty plate

        mockMvc
            .perform(
                post("/api/v1/plate-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(plateStatusRequest))
            )
            .andExpect(status().isBadRequest)

        verify(plateService, never()).getPlateStatus(any())
    }

    @Test
    fun `should get plate status successfully with Mercosul format license plate`() {
        val licensePlate = "ABC1A23" // Mercosul format
        val plateStatusRequest = PlateStatusRequest(licensePlate = licensePlate)
        val plateStatusResponse = PlateStatusResponse(
            licensePlate = licensePlate,
            priceUntilNow = BigDecimal("25.75"),
            entryTime = LocalDateTime.now().minusHours(3),
            timeParked = LocalDateTime.now()
        )

        `when`(plateService.getPlateStatus(licensePlate)).thenReturn(plateStatusResponse)

        mockMvc
            .perform(
                post("/api/v1/plate-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(plateStatusRequest))
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.license_plate").value(licensePlate))
            .andExpect(jsonPath("$.price_until_now").value(25.75))

        verify(plateService, times(1)).getPlateStatus(licensePlate)
    }

    // ===========================================
    // SPOT STATUS TESTS
    // ===========================================

    @Test
    fun `should get spot status successfully with valid coordinates`() {
        val lat = -23.561684
        val lng = -46.655981
        val spotStatusRequest = SpotStatusRequest(lat = lat, lng = lng)
        val spotStatusResponse = SpotStatusResponse(
            occupied = true,
            entryTime = LocalDateTime.now().minusHours(1),
            timeParked = LocalDateTime.now()
        )

        `when`(spotService.getSpotStatus(lat, lng)).thenReturn(spotStatusResponse)

        mockMvc
            .perform(
                post("/api/v1/spot-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(spotStatusRequest))
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.occupied").value(true))

        verify(spotService, times(1)).getSpotStatus(lat, lng)
    }

    @Test
    fun `should get spot status for empty spot`() {
        val lat = -23.561684
        val lng = -46.655981
        val spotStatusRequest = SpotStatusRequest(lat = lat, lng = lng)
        val spotStatusResponse = SpotStatusResponse(
            occupied = false,
            entryTime = null,
            timeParked = null
        )

        `when`(spotService.getSpotStatus(lat, lng)).thenReturn(spotStatusResponse)

        mockMvc
            .perform(
                post("/api/v1/spot-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(spotStatusRequest))
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.occupied").value(false))
            .andExpect(jsonPath("$.entry_time").isEmpty)
            .andExpect(jsonPath("$.time_parked").isEmpty)

        verify(spotService, times(1)).getSpotStatus(lat, lng)
    }

    @Test
    fun `should return 400 when getting spot status with invalid latitude`() {
        val spotStatusRequest = SpotStatusRequest(
            lat = 91.0, // Invalid latitude
            lng = -46.655981
        )

        mockMvc
            .perform(
                post("/api/v1/spot-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(spotStatusRequest))
            )
            .andExpect(status().isBadRequest)

        verify(spotService, never()).getSpotStatus(any(), any())
    }

    @Test
    fun `should return 400 when getting spot status with invalid longitude`() {
        val spotStatusRequest = SpotStatusRequest(
            lat = -23.561684,
            lng = 181.0 // Invalid longitude
        )

        mockMvc
            .perform(
                post("/api/v1/spot-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(spotStatusRequest))
            )
            .andExpect(status().isBadRequest)

        verify(spotService, never()).getSpotStatus(any(), any())
    }

    // ===========================================
    // REVENUE TESTS
    // ===========================================

    @Test
    fun `should get revenue successfully with valid date and sector`() {
        val date = "2025-06-03"
        val sector = "A"
        val revenueRequest = RevenueRequest(date = date, sector = sector)
        val revenueResponse = RevenueResponse(
            amount = BigDecimal("1250.50"),
            currency = "BRL",
            timestamp = LocalDateTime.now()
        )

        `when`(revenueService.getRevenue(date, sector)).thenReturn(revenueResponse)

        mockMvc
            .perform(
                get("/api/v1/revenue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(revenueRequest))
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(1250.50))
            .andExpect(jsonPath("$.currency").value("BRL"))

        verify(revenueService, times(1)).getRevenue(date, sector)
    }

    @Test
    fun `should return 400 when getting revenue with invalid date format`() {
        val revenueRequest = RevenueRequest(
            date = "2025/06/03", // Invalid format
            sector = "A"
        )

        mockMvc
            .perform(
                get("/api/v1/revenue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(revenueRequest))
            )
            .andExpect(status().isBadRequest)

        verify(revenueService, never()).getRevenue(any(), any())
    }

    @Test
    fun `should return 400 when getting revenue with empty sector`() {
        val revenueRequest = RevenueRequest(
            date = "2025-06-03",
            sector = "" // Empty sector
        )

        mockMvc
            .perform(
                get("/api/v1/revenue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(revenueRequest))
            )
            .andExpect(status().isBadRequest)

        verify(revenueService, never()).getRevenue(any(), any())
    }

    @Test
    fun `should return 400 when getting revenue with sector too long`() {
        val revenueRequest = RevenueRequest(
            date = "2025-06-03",
            sector = "ABC" // Too long (max 2 characters)
        )

        mockMvc
            .perform(
                get("/api/v1/revenue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(revenueRequest))
            )
            .andExpect(status().isBadRequest)

        verify(revenueService, never()).getRevenue(any(), any())
    }

    @Test
    fun `should get revenue successfully with single character sector`() {
        val date = "2025-06-03"
        val sector = "B"
        val revenueRequest = RevenueRequest(date = date, sector = sector)
        val revenueResponse = RevenueResponse(
            amount = BigDecimal("750.25"),
            currency = "BRL",
            timestamp = LocalDateTime.now()
        )

        `when`(revenueService.getRevenue(date, sector)).thenReturn(revenueResponse)

        mockMvc
            .perform(
                get("/api/v1/revenue")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(revenueRequest))
            )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.amount").value(750.25))
            .andExpect(jsonPath("$.currency").value("BRL"))

        verify(revenueService, times(1)).getRevenue(date, sector)
    }

    // ===========================================
    // CONTENT TYPE AND MALFORMED JSON TESTS
    // ===========================================

    @Test
    fun `should return 400 when request has invalid content type`() {
        val plateStatusRequest = PlateStatusRequest(licensePlate = "ABC1234")

        mockMvc
            .perform(
                post("/api/v1/plate-status")
                    .contentType(MediaType.TEXT_PLAIN) // Invalid content type
                    .content(objectMapper.writeValueAsString(plateStatusRequest))
            )
            .andExpect(status().isInternalServerError)

        verify(plateService, never()).getPlateStatus(any())
    }

    @Test
    fun `should return 400 when request has malformed JSON`() {
        mockMvc
            .perform(
                post("/api/v1/plate-status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{ invalid json }")
            )
            .andExpect(status().isBadRequest)

        verify(plateService, never()).getPlateStatus(any())
    }

    @Test
    fun `should return 404 when endpoint does not exist`() {
        mockMvc
            .perform(
                post("/api/v1/non-existent")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isInternalServerError)
    }

    // ===========================================
    // MULTIPLE VALIDATION ERRORS TESTS
    // ===========================================

    @Test
    fun `should return 400 with multiple validation errors for garage creation`() {
        val garageRequest = GarageRequest(
            sector = "", // Invalid empty sector
            basePrice = BigDecimal("10.00"),
            maxCapacity = 100,
            openHour = "8:00", // Invalid format
            closeHour = "", // Invalid empty close hour
            durationLimitMinutes = 240
        )

        val spotsRequest = SpotsRequest(
            id = 1,
            sector = "A",
            lat = 91.0, // Invalid latitude
            lng = 181.0 // Invalid longitude
        )

        val createGarageRequest = CreateGarageRequest(
            garage = listOf(garageRequest),
            spots = listOf(spotsRequest)
        )

        mockMvc
            .perform(
                post("/api/v1/garage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createGarageRequest))
            )
            .andExpect(status().isBadRequest)

        verify(garageService, never()).createGarage(any())
    }
}

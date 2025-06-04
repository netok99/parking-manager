package com.parking.parking_manager.infrastructure.adapters.output

import com.parking.parking_manager.application.port.output.VehicleEventRepository
import com.parking.parking_manager.domain.entity.VehicleEvent
import com.parking.parking_manager.domain.entity.VehicleEvents
import com.parking.parking_manager.domain.entity.VehicleStatus
import java.sql.Timestamp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Repository

@Repository
class VehicleEventRepositoryImpl(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : VehicleEventRepository {

    private val vehicleRowMapper = RowMapper<VehicleEvent> { resulSet, _ ->
        VehicleEvent(
            id = resulSet.getLong("id"),
            licensePlate = resulSet.getString("license_plate"),
            eventType = VehicleStatus.valueOf(resulSet.getString("event_type")),
            latitude = resulSet.getDouble("latitude"),
            longitude = resulSet.getDouble("longitude"),
            spotId = resulSet.getLong("spot_id"),
            sectorId = resulSet.getLong("sector_id"),
            sectorBasePrice = resulSet.getBigDecimal("sector_base_price"),
            dynamicRate = resulSet.getBigDecimal("dynamic_rate"),
            entryAt = resulSet.getTimestamp("entry_at").toLocalDateTime(),
            createdAt = resulSet.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = resulSet.getTimestamp("updated_at").toLocalDateTime()
        )
    }

    override fun getByLicensePlate(licensePlate: String): VehicleEvents {
        val sql = """
            SELECT id, license_plate, event_type, latitude, longitude, spot_id, sector_id, sector_base_price, 
                dynamic_rate, entry_at, created_at, updated_at
            FROM parking.vehicle_event
            WHERE license_plate = :licensePlate
            ORDER BY created_at ASC
        """
        val params = MapSqlParameterSource("licensePlate", licensePlate)
        return namedParameterJdbcTemplate.query(sql, params, vehicleRowMapper)
    }

    override fun getBySpotId(spotId: Long): VehicleEvents {
        val sql = """
            SELECT id, license_plate, event_type, latitude, longitude, spot_id, sector_id, sector_base_price, 
                dynamic_rate, entry_at, created_at, updated_at
            FROM parking.vehicle_event
            WHERE spot_id  = :spotId
            ORDER BY created_at ASC
        """
        val params = MapSqlParameterSource("spotId", spotId)
        return namedParameterJdbcTemplate.query(sql, params, vehicleRowMapper)
    }

    override fun getBySectorIdAndDate(sectorId: Long, date: String): VehicleEvents {
        val localDate = LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        val startOfDay = localDate.atStartOfDay()
        val endOfDay = localDate.atTime(23, 59, 59, 999999999)
        val sql = """
            SELECT id, license_plate, event_type, latitude, longitude, spot_id, sector_id, sector_base_price, 
                dynamic_rate, entry_at, created_at, updated_at
            FROM parking.vehicle_event
            WHERE sector_id = :sectorId
            AND entry_at >= :startDate 
            AND updated_at <= :endDate
            ORDER BY created_at ASC
        """
        val params = MapSqlParameterSource("sectorId", sectorId)
            .addValue("startDate", startOfDay)
            .addValue("endDate", endOfDay)
        return namedParameterJdbcTemplate.query(sql, params, vehicleRowMapper)
    }

    override fun save(vehicleEvent: VehicleEvent) {
        val sql = """
            INSERT INTO parking.vehicle_event (license_plate, event_type, latitude, longitude, spot_id, sector_id, 
                sector_base_price, dynamic_rate, entry_at, created_at, updated_at)
            VALUES (:licensePlate, :eventType, :latitude, :longitude, :spotId, :sectorId, :sectorBasePrice, 
                :dynamicRate, :entryAt, :createdAt, :updatedAt)
        """
        val params = MapSqlParameterSource().apply {
            addValue("licensePlate", vehicleEvent.licensePlate)
            addValue("eventType", vehicleEvent.eventType.name)
            addValue("latitude", vehicleEvent.latitude)
            addValue("longitude", vehicleEvent.longitude)
            addValue("spotId", vehicleEvent.spotId)
            addValue("sectorId", vehicleEvent.sectorId)
            addValue("sectorBasePrice", vehicleEvent.sectorBasePrice)
            addValue("dynamicRate", vehicleEvent.dynamicRate)
            addValue("entryAt", Timestamp.valueOf(vehicleEvent.entryAt))
            addValue("createdAt", Timestamp.valueOf(vehicleEvent.createdAt))
            addValue("updatedAt", Timestamp.valueOf(vehicleEvent.updatedAt))
        }
        val keyHolder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.update(sql, params, keyHolder, arrayOf("id"))
    }
}

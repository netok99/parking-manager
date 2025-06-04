package com.parking.parking_manager.infrastructure.adapters.output

import com.parking.parking_manager.application.port.output.SpotRepository
import com.parking.parking_manager.domain.entity.Spot
import com.parking.parking_manager.infrastructure.exception.SpotNotFoundException
import java.sql.Timestamp
import java.sql.Types
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SpotRepositoryImpl(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) : SpotRepository {

    private val spotRowMapper = RowMapper<Spot> { resultSet, _ ->
        Spot(
            id = resultSet.getLong("id"),
            sectorId = resultSet.getLong("sector_id"),
            latitude = resultSet.getDouble("latitude"),
            longitude = resultSet.getDouble("longitude"),
            createdAt = resultSet.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = resultSet.getTimestamp("updated_at").toLocalDateTime()
        )
    }

    override fun findByCoordinates(latitude: Double, longitude: Double): Spot {
        val sql = """
            SELECT id, sector_id, latitude, longitude, created_at, updated_at
            FROM parking.spot
            WHERE latitude = :latitude AND longitude = :longitude
        """
        val params = MapSqlParameterSource().apply {
            addValue("latitude", latitude)
            addValue("longitude", longitude)
        }

        val spotNotFoundException = SpotNotFoundException("Spot not found")
        try {
            val spot = namedParameterJdbcTemplate.queryForObject(sql, params, spotRowMapper)
            if (spot == null) throw spotNotFoundException
            return spot
        } catch (_: EmptyResultDataAccessException) {
            throw spotNotFoundException
        }
    }

    override fun findById(id: Long): Spot {
        val sql = """
            SELECT id, sector_id, latitude, longitude, created_at, updated_at
            FROM parking.spot
            WHERE id = :id
        """
        val params = MapSqlParameterSource("id", id)
        val spot = namedParameterJdbcTemplate.queryForObject(sql, params, spotRowMapper)
        if (spot == null) throw SpotNotFoundException("Spot not found")
        return spot
    }

    override fun findBySector(sector: String): List<Spot> {
        val sql = """
            SELECT id, sector_id, latitude, longitude, created_at, updated_at
            FROM parking.spot 
            WHERE sector = :sector
            ORDER BY id
        """
        val params = MapSqlParameterSource("sector", sector)
        return namedParameterJdbcTemplate.query(sql, params, spotRowMapper)
    }

    override fun save(spot: Spot): Spot {
        val sql = """
            INSERT INTO parking.spot (sector_id, latitude, longitude, created_at, updated_at)
            VALUES (:sectorId, :latitude, :longitude, :isOccupied, :createdAt, :updatedAt)
            ON CONFLICT (id) 
            DO UPDATE SET 
                sector_id = :sectorId,
                latitude = :latitude,
                longitude = :longitude,
                updated_at = :updatedAt
        """
        val params = MapSqlParameterSource().apply {
            addValue("sectorId", spot.sectorId)
            addValue("latitude", spot.latitude)
            addValue("longitude", spot.longitude)
            addValue("isOccupied", spot.isOccupied, Types.BOOLEAN)
            addValue("createdAt", Timestamp.valueOf(spot.createdAt))
            addValue("updatedAt", Timestamp.valueOf(spot.updatedAt))
        }
        namedParameterJdbcTemplate.update(sql, params)
        return spot
    }

    override fun deleteAll() {
        namedParameterJdbcTemplate.update("DELETE FROM parking.spot;", emptyMap<String, Any>())
    }
}

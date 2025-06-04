package com.parking.parking_manager.infrastructure.adapters.output

import com.parking.parking_manager.application.port.output.SectorRepository
import com.parking.parking_manager.domain.entity.Sector
import com.parking.parking_manager.infrastructure.exception.SectorNotFoundException
import java.sql.Timestamp
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class SectorRepositoryImpl(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) : SectorRepository {

    private val sectorRowMapper = RowMapper<Sector> { resultSet, _ ->
        Sector(
            id = resultSet.getLong("id"),
            sectorCode = resultSet.getString("sector_code"),
            basePrice = resultSet.getBigDecimal("base_price"),
            maxCapacity = resultSet.getInt("max_capacity"),
            openHour = resultSet.getTime("open_hour").toLocalTime(),
            closeHour = resultSet.getTime("close_hour").toLocalTime(),
            durationLimitMinutes = resultSet.getInt("duration_limit_minutes"),
            currencyCode = resultSet.getString("currency_code"),
            createdAt = resultSet.getTimestamp("created_at").toLocalDateTime(),
            updatedAt = resultSet.getTimestamp("updated_at").toLocalDateTime()
        )
    }

    override fun getById(sectorId: Long): Sector {
        val sql = """
            SELECT id, sector_code, base_price, max_capacity, open_hour, close_hour, duration_limit_minutes, 
                currency_code, created_at, updated_at
            FROM parking.sector 
            WHERE id = :sectorId;
        """
        val params = MapSqlParameterSource("sectorId", sectorId)
        try {
            val sector = namedParameterJdbcTemplate.queryForObject(sql, params, sectorRowMapper)
            if (sector == null) throw SectorNotFoundException("Sector not found")
            return sector
        } catch (_: EmptyResultDataAccessException) {
            throw SectorNotFoundException("Sector not found")
        }
    }

    override fun getByCode(sectorCode: String): Sector {
        val sql = """
            SELECT id, sector_code, base_price, max_capacity, open_hour, close_hour, duration_limit_minutes, 
                currency_code, created_at, updated_at
            FROM parking.sector 
            WHERE sector_code = :sectorCode;
        """

        val params = MapSqlParameterSource("sectorCode", sectorCode)
        val sector = namedParameterJdbcTemplate.queryForObject(sql, params, sectorRowMapper)
        if (sector == null) throw SectorNotFoundException("Sector not found")
        return sector
    }

    override fun save(sector: Sector) {
        val sql = """
            INSERT INTO parking.sector (sector_code, base_price, max_capacity, open_hour, close_hour, 
                duration_limit_minutes, currency_code, created_at, updated_at)
            VALUES (:sectorCode, :basePrice, :maxCapacity, :openHour, :closeHour, :durationLimitMinutes, :currencyCode,
                :createdAt, :updatedAt)
        """

        val params = MapSqlParameterSource().apply {
            addValue("sectorCode", sector.sectorCode)
            addValue("basePrice", sector.basePrice)
            addValue("maxCapacity", sector.maxCapacity)
            addValue("openHour", sector.openHour)
            addValue("closeHour", sector.closeHour)
            addValue("durationLimitMinutes", sector.durationLimitMinutes)
            addValue("currencyCode", sector.currencyCode)
            addValue("createdAt", Timestamp.valueOf(sector.createdAt))
            addValue("updatedAt", Timestamp.valueOf(sector.updatedAt))
        }
        namedParameterJdbcTemplate.update(sql, params)
    }

    override fun deleteAll() {
        namedParameterJdbcTemplate.update("DELETE FROM parking.sector;", emptyMap<String, Any>())
    }
}

package com.parking.parking_manager.infrastructure.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import javax.sql.DataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

@Configuration
@ConfigurationProperties(prefix = "spring.datasource.hikari")
class HikariProperties {
    var poolName: String = "DefaultHikariPool"
    var minimumIdle: Int = 5
    var maximumPoolSize: Int = 20
    var idleTimeout: Long = 300000
    var connectionTimeout: Long = 20000
    var maxLifetime: Long = 1200000
    var leakDetectionThreshold: Long = 0
    var connectionTestQuery: String? = null
}

const val DRIVER_CLASS_NAME = "org.postgresql.Driver"

@Configuration
class DatabaseConfig(private val hikariProperties: HikariProperties) {

    @Bean
    @Primary
    fun dataSource() = HikariDataSource(
        HikariConfig()
            .apply {
                jdbcUrl = System.getProperty(
                    "spring.datasource.url",
                    "jdbc:postgresql://localhost:5432/parking_management"
                )
                username = System.getProperty("spring.datasource.username", "parking_user")
                password = System.getProperty("spring.datasource.password", "parking_pass")
                driverClassName = DRIVER_CLASS_NAME
                poolName = hikariProperties.poolName
                minimumIdle = hikariProperties.minimumIdle
                maximumPoolSize = hikariProperties.maximumPoolSize
                idleTimeout = hikariProperties.idleTimeout
                connectionTimeout = hikariProperties.connectionTimeout
                maxLifetime = hikariProperties.maxLifetime
                if (hikariProperties.leakDetectionThreshold > 0) {
                    leakDetectionThreshold = hikariProperties.leakDetectionThreshold
                }
                hikariProperties.connectionTestQuery?.let {
                    connectionTestQuery = it
                }
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("reWriteBatchedInserts", "true")
            }
    )

    @Bean
    fun jdbcTemplate(dataSource: DataSource) = JdbcTemplate(dataSource)

    @Bean
    fun namedParameterJdbcTemplate(dataSource: DataSource) = NamedParameterJdbcTemplate(dataSource)
}

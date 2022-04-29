package com.shinonometn.music.server.configuration

import com.shinonometn.koemans.exposed.database.MariaDB
import com.shinonometn.koemans.exposed.database.sqlDatabase
import com.shinonometn.koemans.exposed.datasource.HikariDatasource
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class DatabaseConfiguration(
    @Value("\${application.database.host}") private val host : String,
    @Value("\${application.database.port}") private val port : Int,
    @Value("\${application.database.name}") private val dbName : String,
    @Value("\${application.database.username}") private val dbUsername : String,
    @Value("\${application.database.password}") private val dbPassword : String
) {

    @Bean
    open fun database() = sqlDatabase(MariaDB) {

        host(host, port)

        database = dbName
        username = dbUsername
        password = dbPassword
        dataSource = HikariDatasource {
            maximumPoolSize = 10
            minimumIdle = 1
        }
    }
}
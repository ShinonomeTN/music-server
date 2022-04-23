package com.shinonometn.music.server.data

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserData {
    object Table : LongIdTable("tb_user") {
        val username = varchar("username", 255)
        val password = varchar("password", 255)
        val email = varchar("email", 255)
        val avatar = varchar("avatar", 255)
        val description = varchar("description", 255)
        val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
        val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    }
}
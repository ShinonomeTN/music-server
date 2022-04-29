package com.shinonometn.music.server.security.data

import com.shinonometn.koemans.exposed.countBy
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object SessionData {
    fun lockSessionId(sessionId: String): Boolean {
        return Table.select { Table.colSessionId eq sessionId }.forUpdate().count() > 0
    }

    fun updateSession(sessionId: String, block: Table.(UpdateStatement) -> Unit): Int {
        return Table.update({ Table.colSessionId eq sessionId }) { block(it) }
    }

    fun deleteBySessionId(sessionId: String): Int {
        return Table.deleteWhere { Table.colSessionId eq sessionId }
    }

    fun isSessionValid(sessionId: String): Boolean {
        return Table.countBy(Table.id) { (Table.colSessionId eq sessionId) and (Table.colExpireAt greaterEq LocalDateTime.now()) } > 0
    }

    fun clearExpired(): Int {
        return Table.deleteWhere { Table.colExpireAt lessEq LocalDateTime.now() }
    }

    object Table : LongIdTable("tb_session") {
        val colSessionId = varchar("session_id", 128).uniqueIndex()
        val colUserId = long("user_id")
        val colExpireAt = datetime("expire_at")
        val colCreateAt = datetime("create_at").clientDefault { LocalDateTime.now() }
        val colIpAddress = varchar("ip_address", 64)
        val colUserAgent = varchar("user_agent", 255)
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var sessionId by Table.colSessionId
        var userId by Table.colUserId
        var expireAt by Table.colExpireAt
        var ipAddress by Table.colIpAddress
        var createAt by Table.colCreateAt
        var userAgent by Table.colUserAgent
    }

    class Bean(entity: Entity) {
        val id = entity.id.value
        val sessionId = entity.sessionId
        val userId = entity.userId
        val expireAt = entity.expireAt
        val ipAddress = entity.ipAddress
        val createAt = entity.createAt
        val userAgent = entity.userAgent
    }
}
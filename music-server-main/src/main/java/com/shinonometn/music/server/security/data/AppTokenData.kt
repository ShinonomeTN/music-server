package com.shinonometn.music.server.security.data

import com.shinonometn.koemans.exposed.countBy
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object AppTokenData {
    fun update(tokenId: Long, function: Table.(UpdateStatement) -> Unit): Int {
        return Table.update({ Table.id eq tokenId }, body = function)
    }

    fun isTokenValid(tokenId: Long): Boolean {
        return Table.countBy(Table.id) { (Table.id eq tokenId) and (Table.colExpiredAt greaterEq LocalDateTime.now() ) } > 0
    }

    fun clearExpired(): Int {
        return Table.deleteWhere { Table.colExpiredAt lessEq LocalDateTime.now() }
    }

    object Table : LongIdTable("tb_token") {
        val colUserAgent = varchar("user_agent", 255)
        val colUserId = long("user_id")
        val colScope = text("scope")
        val colExpiredAt = datetime("expired_at")
        val colCreateAt = datetime("create_at").clientDefault { LocalDateTime.now() }
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var userAgent by Table.colUserAgent
        var userId by Table.colUserId
        var scope by Table.colScope
        var expiredAt by Table.colExpiredAt
        var createAt by Table.colCreateAt
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val userAgent = entity.userAgent
        val userId = entity.userId
        val scope = entity.scope
        val expiredAt = entity.expiredAt
        val createAt = entity.createAt
    }
}
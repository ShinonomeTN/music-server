package com.shinonometn.music.server.platform.security.data

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.countBy
import com.shinonometn.koemans.exposed.pagingBy
import com.shinonometn.music.server.commons.transformJsonNode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object UserData {
    fun usernameExists(username: String): Boolean {
        return Table.select { Table.colUsername eq username }.forUpdate().count() > 0
    }

    fun findById(id: Long): Bean? {
        return Entity.findById(id)?.let { Bean(it) }
    }

    fun findAll(paging: PageRequest): Page<Bean> {
        return Table.selectAll().pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun update(id: Long, block: Table.(UpdateStatement) -> Unit): Int {
        return Table.update({ Table.id eq id }, body= block)
    }

    fun userCount(): Long {
        return Table.countBy(Table.id)
    }

    object Table : LongIdTable("tb_user") {
        val colUsername = varchar("username", 64)
        val colPassword = varchar("password", 255)
        val colEnabled = bool("enabled").default(true)

        val colPrivateProfile = bool("private_profile").default(false)

        val colNickname = varchar("nickname", 255).nullable()
        val colEmail = varchar("email", 255).nullable()
        val colAvatar = varchar("avatar", 255).nullable()

        val colResources = text("resources").default("{}")
        val colCreatedAt = datetime("created_at").clientDefault { LocalDateTime.now() }
        val colUpdatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table) {
            fun findByUsername(username: String): Entity? {
                return find { Table.colUsername eq username }.firstOrNull()
            }
        }

        var username by Table.colUsername
        var password by Table.colPassword
        var nickname by Table.colNickname
        var enabled by Table.colEnabled
        var privateProfile by Table.colPrivateProfile
        var email by Table.colEmail
        var avatar by Table.colAvatar
        var resources: JsonNode by Table.colResources.transformJsonNode()
        var createdAt by Table.colCreatedAt
        var updatedAt by Table.colUpdatedAt

        fun toProfileBean() : Any {
            return if(privateProfile) PrivateProfileBean(this) else PublicProfileBean(this)
        }
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val username = entity.username
        val nickname = entity.nickname
        val email = entity.email
        val privateProfile = entity.privateProfile
        val enabled = entity.enabled
        val avatar = entity.avatar
        val resources = entity.resources
        val createdAt = entity.createdAt
        val updatedAt = entity.updatedAt
    }

    class PublicProfileBean(entity: Entity) {
        val id = entity.id.value
        val username = entity.username
        val nickname = entity.nickname
        val email = entity.email
        val privateProfile = entity.privateProfile
        val enabled = entity.enabled
        val avatar = entity.avatar

        val createdAt = entity.createdAt
    }

    class PrivateProfileBean(entity: Entity) {
        val id = entity.id.value
        val username = entity.username
        val nickname = entity.nickname

        val privateProfile = entity.privateProfile

        val avatar = entity.avatar
    }
}
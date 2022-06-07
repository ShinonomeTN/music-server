package com.shinonometn.music.server.library.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.shinonometn.koemans.exposed.countBy
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserSubscribedPlaylistData {
    fun existsByUserIdAndPlaylistId(userId: Long, playlistId: Long): Boolean {
        return Table.countBy(Table.id) { (Table.colUserId eq userId) and (Table.colPlaylistId eq playlistId) } > 0
    }

    fun create(userId: Long, playlistId: Long): Int {
        return Table.insert {
            it[colUserId] = userId
            it[colPlaylistId] = EntityID(playlistId, PlaylistData.Table)
        }.insertedCount
    }

    fun findByUserIdAndPlaylistId(userId: Long, playlistId: Long): Bean? {
        return Entity.find { (Table.colUserId eq userId) and (Table.colPlaylistId eq playlistId) }.map { Bean(it) }.firstOrNull()
    }

    fun deleteBy(userId: Long, playlistId: Long): Int {
        return Table.deleteWhere { (Table.colUserId eq userId) and (Table.colPlaylistId eq playlistId) }
    }

    object Table : LongIdTable(name = "tb_library_playlist_sub") {

        val colUserId = long("user_id").index("i_usp_user_id")

        val colPlaylistId = reference("playlist_id", PlaylistData.Table)
            .index("i_usp_playlist_id").nullable()

        val colCreateDate = datetime("subscribed_date")
            .clientDefault { LocalDateTime.now() }
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : EntityClass<Long, Entity>(Table)

        var userId by Table.colUserId
        var playlist by PlaylistData.Entity optionalReferencedOn Table.colPlaylistId
        var playlistId by Table.colPlaylistId
        var createDate by Table.colCreateDate
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val userId = entity.userId
        @JsonIgnore
        val playlist = entity.playlist
        val playlistId = entity.playlistId?.value
        val createDate = entity.createDate
    }
}
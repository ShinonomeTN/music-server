package com.shinonometn.music.server.library.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.shinonometn.koemans.exposed.*
import com.shinonometn.music.server.media.data.TrackData
import com.shinonometn.music.server.platform.security.data.UserData
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserFavoriteTrackData {
    fun findByUserId(userId: Long, sorting: SortRequest, paging: PageRequest): Page<Bean> {
        return Table.select { Table.colUserId eq userId }.orderBy(sorting).pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun isRelationshipExists(userId: Long, trackId: Long) : Boolean {
        return Table.countBy(Table.id) { (Table.colUserId eq userId) and (Table.colTrackId eq trackId) } > 0
    }

    fun createRelationship(userId: Long, trackId: Long): Int {
        if(isRelationshipExists(userId, trackId)) return 0
        return Table.insert {
            it[colUserId] = UserData.entityIdOf(userId)
            it[colTrackId] = TrackData.entityIdOf(trackId)
            it[colFavouriteDate] = LocalDateTime.now()
        }.insertedCount
    }

    fun removeRelationship(userId: Long, trackId: Long): Int {
        return Table.deleteWhere { (Table.colUserId eq userId) and (Table.colTrackId eq trackId) }
    }

    val sortMapping = SortOptionMapping {
        "date" associateTo Table.colFavouriteDate defaultOrder SortOrder.DESC
    }

    object Table : LongIdTable("tb_library_track") {
        val colUserId = reference("user_id", UserData.Table)
        val colTrackId = reference("track_id", TrackData.Table).nullable()
        val colFavouriteDate = datetime("favourite_date").clientDefault { LocalDateTime.now() }
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object: EntityClass<Long, Entity>(Table)

        var userId by Table.colUserId
        var trackId by Table.colTrackId
        val track by TrackData.Entity optionalReferencedOn Table.colTrackId
        var favouriteDate by Table.colFavouriteDate
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val userId = entity.userId.value
        val trackId = entity.trackId?.value
        @JsonIgnore
        val track = entity.track?.let { TrackData.Bean(it) }
        val favouriteDate = entity.favouriteDate
    }
}
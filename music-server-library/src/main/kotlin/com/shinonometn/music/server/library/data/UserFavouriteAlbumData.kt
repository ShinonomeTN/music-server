package com.shinonometn.music.server.library.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.shinonometn.koemans.exposed.*
import com.shinonometn.music.server.media.data.AlbumData
import com.shinonometn.music.server.platform.security.data.UserData
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserFavouriteAlbumData {
    fun findByUserId(userId: Long, sorting: SortRequest, paging: PageRequest): Page<Bean> {
        return Table.select{ Table.colUserId eq userId }.orderBy(sorting).pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun isRelationshipExists(userId: Long, albumId: Long): Boolean {
        return Table.countBy(Table.id) { (Table.colUserId eq userId) and (Table.colAlbumId eq albumId) } > 0
    }

    fun createRelationship(userId: Long, albumId: Long): Int {
        if(isRelationshipExists(userId, albumId)) return 0
        return Table.insert {
            it[colAlbumId] = AlbumData.entityIdOf(albumId)
            it[colUserId] = UserData.entityIdOf(userId)
            it[colFavouriteDate] = LocalDateTime.now()
        }.insertedCount
    }

    fun removeRelationship(userId : Long, albumId: Long): Int {
        return Table.deleteWhere { (Table.colUserId eq userId) and (Table.colAlbumId eq albumId) }
    }

    fun findByUserIdAndAlbumId(userId: Long, albumId: Long): Bean? {
        return Entity.find { (Table.colUserId eq userId) and (Table.colAlbumId eq albumId) }.map {
            Bean(it)
        }.firstOrNull()
    }

    val sortMapping = SortOptionMapping {
        "favourite_date" associateTo Table.colFavouriteDate defaultOrder SortOrder.DESC
    }

    object Table : LongIdTable("tb_library_album") {
        val colUserId = reference("user_id", UserData.Table)
        val colAlbumId = reference("album_id", AlbumData.Table).nullable()
        val colFavouriteDate = datetime("favourite_date").clientDefault { LocalDateTime.now() }
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : EntityClass<Long, Entity>(Table)
        var userId by Table.colUserId
        var albumId by Table.colAlbumId
        var album by AlbumData.Entity optionalReferencedOn Table.colAlbumId
        var favouriteDate by Table.colFavouriteDate
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val userId = entity.userId.value
        val albumId = entity.albumId?.value
        @JsonIgnore
        val album = entity.album?.let { AlbumData.Bean(it) }
        val favouriteDate = entity.favouriteDate
    }
}
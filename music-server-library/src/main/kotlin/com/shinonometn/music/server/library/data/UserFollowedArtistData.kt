package com.shinonometn.music.server.library.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.shinonometn.koemans.exposed.*
import com.shinonometn.music.server.media.data.ArtistData
import com.shinonometn.music.server.platform.security.data.UserData
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserFollowedArtistData {

    fun isRelationshipExists(artistId: Long, userId: Long) : Boolean {
        return Table.countBy(Table.id) { (Table.colArtistId eq artistId) and (Table.colUserId eq userId) } > 0
    }

    fun createRelationship(artistId: Long, userId: Long): Int {
        if(isRelationshipExists(artistId, userId)) return 0
        return Table.insert {
            it[colUserId] = UserData.entityIdOf(userId)
            it[colArtistId] = ArtistData.entityIdOf(artistId)
            it[colFollowDate] = LocalDateTime.now()
        }.insertedCount
    }

    fun deleteRelationship(artistId: Long, userId: Long) : Int {
        return Table.deleteWhere {
            (Table.colArtistId eq artistId) and (Table.colUserId eq userId)
        }
    }

    fun updateFetchTime(userId: Long, artistId: Long, datetime: LocalDateTime): Int {
        return Table.update({ (Table.colUserId eq userId) and (Table.colArtistId eq artistId) }) {
            it[colUpdateDate] = datetime
        }
    }

    fun artistUpdateFor(userId: Long, artistId: Long): Bean? {
        return Table.select { (Table.colUserId eq userId) and (Table.colArtistId eq artistId) }.map {
            Bean(Entity.wrapRow(it))
        }.firstOrNull()
    }

    fun findAllByUserId(userId: Long, sorting: SortRequest, paging: PageRequest): Page<Bean> {
        val tbArtist = ArtistData.Table
        return Table.join(tbArtist, JoinType.LEFT, tbArtist.id eq Table.colArtistId) {
            Table.colUserId eq userId
        }.selectAll().orderBy(sorting).pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    object Table : LongIdTable(name = "tb_library_artists") {
        val colUserId = reference("user_id", UserData.Table)
        val colArtistId = reference("artist_id", ArtistData.Table).nullable()
        val colFollowDate = datetime("follow_date").clientDefault { LocalDateTime.now() }
        val colUpdateDate = datetime("update_date").clientDefault { LocalDateTime.now() }
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : EntityClass<Long, Entity>(Table)
        var userId by Table.colUserId
        val artist by ArtistData.Entity optionalReferencedOn Table.colArtistId
        var artistId by Table.colArtistId
        var followDate by Table.colFollowDate
        var updateDate by Table.colUpdateDate
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val userId = entity.userId.value
        @JsonIgnore
        val artist = entity.artist?.let { ArtistData.Bean(it) }
        val artistId = entity.artistId?.value
        val followDate = entity.followDate
        val updateDate = entity.updateDate
    }
}
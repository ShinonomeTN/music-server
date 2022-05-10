package com.shinonometn.music.server.media.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteWhere

object TrackArtistData {
    fun deleteByArtistId(id: Long): Int {
        return Table.deleteWhere { Table.colArtist eq id }
    }

    fun deleteRelationshipsByTrackId(id: Long): Int {
        return Table.deleteWhere { Table.colTrack eq id }
    }

    object Table : LongIdTable("tb_track_artist") {
        val colTrack = reference("track_id", TrackData.Table)
        val colArtist = reference("artist_id", ArtistData.Table)
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var track by TrackData.Entity referencedOn Table.colTrack
        var artist by ArtistData.Entity referencedOn Table.colArtist
    }
}
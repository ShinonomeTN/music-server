package com.shinonometn.music.server.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

object TrackArtistData {
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
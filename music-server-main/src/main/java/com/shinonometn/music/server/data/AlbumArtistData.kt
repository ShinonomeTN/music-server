package com.shinonometn.music.server.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert

object AlbumArtistData {
    fun createRelation(albumId: Long, artistId: Long) {
        Table.insert {
            it[this.colAlbumId] = albumId
            it[this.colArtistId] = artistId
        }
    }

    fun removeAllRelationsByAlbumId(id: Long) {
        Table.deleteWhere {
            Table.colAlbumId eq id
        }
    }

    object Table : LongIdTable("tb_album_artist") {
        val colAlbumId = reference("album_id", AlbumData.Table).index()
        val colArtistId = reference("artist_id", ArtistData.Table).index()
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var album by AlbumData.Entity referencedOn Table.colAlbumId
        var artist by ArtistData.Entity referencedOn Table.colArtistId
    }
}
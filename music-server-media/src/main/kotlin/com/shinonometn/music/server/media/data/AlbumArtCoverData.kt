package com.shinonometn.music.server.media.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert

object AlbumArtCoverData {
    fun createRelation(albumId: Long, coverArtId: Long) {
        Table.insert {
            it[this.colAlbumId] = albumId
            it[this.colCoverArtId] = coverArtId
        }
    }

    fun removeAllRelationsByAlbumId(albumId: Long): Int = Table.deleteWhere {
        Table.colAlbumId eq albumId
    }

    fun removeAllRelationsByCoverId(id: Long): Int {
        return Table.deleteWhere { Table.colCoverArtId eq id }
    }

    object Table : LongIdTable("tb_album_art_cover") {
        val colAlbumId = reference("album_id", AlbumData.Table).index()
        val colCoverArtId = reference("art_cover_id", CoverArtData.Table).index()
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var album by AlbumData.Entity referencedOn Table.colAlbumId
        var coverArt by CoverArtData.Entity referencedOn Table.colCoverArtId
    }
}
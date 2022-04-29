package com.shinonometn.music.server.media.data

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.pagingBy
import com.shinonometn.music.server.commons.LongIdMetaDataTable
import com.shinonometn.music.server.commons.transformJsonNode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll

/*
* Tracks in Album
* */
object TrackData {
    fun listAll(paging: PageRequest): Page<Bean> {
        return Table.selectAll().pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun getById(id: Long): Bean? {
        return Entity.findById(id)?.let { Bean(it) }
    }

    fun findAllByAlbumId(id: Long): List<Bean> {
        return Table.select {
            Table.colAlbumId eq id
        }.orderBy(Table.colDiskNumber to SortOrder.ASC, Table.colTrackNumber to SortOrder.ASC).map { Bean(Entity.wrapRow(it)) }
    }

    object Table : LongIdMetaDataTable("tb_track_data") {
        val colAlbumId = reference("album_id", AlbumData.Table).nullable()
        val colTitle = varchar("title", 255)
        val colDiskNumber = integer("disk_number").nullable()
        val colTrackNumber = integer("track_number").nullable()
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var title by Table.colTitle

        val artists by ArtistData.Entity via TrackArtistData.Table

        var diskNumber by Table.colDiskNumber
        var trackNumber by Table.colTrackNumber

        var albumId by Table.colAlbumId
        var album by AlbumData.Entity optionalReferencedOn Table.colAlbumId

        val recordings by RecordingData.Entity referrersOn RecordingData.Table.colTrackId

        var metaData: JsonNode by Table.colMetaData.transformJsonNode()
    }

    class Bean(entity: Entity) {
        val id = entity.id.value

        val title = entity.title

        val artists = entity.artists.map { ArtistData.Bean(it) }

        val diskNumber = entity.diskNumber
        val trackNumber = entity.trackNumber

        val albumId = entity.albumId?.value
        val album = entity.album?.let { AlbumData.Bean(it) }

        val recordings = entity.recordings.map { RecordingData.Bean(it) }

        val metaData = entity.metaData
    }
}
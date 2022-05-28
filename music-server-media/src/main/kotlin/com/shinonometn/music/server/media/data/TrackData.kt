package com.shinonometn.music.server.media.data

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.koemans.exposed.*
import com.shinonometn.music.server.commons.LongIdMetaDataTable
import com.shinonometn.music.server.commons.transformJsonNode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

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

    fun deleteById(id: Long): Int {
        return Table.deleteWhere { Table.id eq id }
    }

    fun removeAlbumRelation(id : Long) : Int {
        return Table.update({ Table.colAlbumId eq id }) { it[colAlbumId] = null }
    }

    fun findAll(paging: PageRequest, filtering: FilterRequest, sorting: SortRequest): Page<Bean> {
        return Table.selectBy(filtering).orderBy(sorting).pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    val filteringOptions = FilterOptionMapping {
        "album_id" means { Table.colAlbumId eq it.asString().toLong() }
        "title" means { Table.colTitle eq it.asString() }
        "disk_number" means { Table.colDiskNumber eq it.asString().toIntOrNull() }
        "track_number" means { Table.colTrackNumber eq it.asString().toIntOrNull() }
    }

    val sortOptions = SortOptionMapping {
        "create_date" associateTo Table.colCreateDate
        "disk_number" associateTo Table.colDiskNumber
        "track_number" associateTo Table.colTrackNumber
    }

    object Table : LongIdMetaDataTable("tb_track_data") {
        val colAlbumId = reference("album_id", AlbumData.Table).nullable()
        val colTitle = varchar("title", 255)
        val colDiskNumber = integer("disk_number").nullable()
        val colTrackNumber = integer("track_number").nullable()
        val colCreateDate = datetime("create_date").clientDefault { LocalDateTime.now() }
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var title by Table.colTitle

        val artists by ArtistData.Entity via TrackArtistRelation.Table

        var diskNumber by Table.colDiskNumber
        var trackNumber by Table.colTrackNumber

        var albumId by Table.colAlbumId
        var album by AlbumData.Entity optionalReferencedOn Table.colAlbumId

        val recordings by RecordingData.Entity referrersOn RecordingData.Table.colTrackId

        var metaData: JsonNode by Table.colMetaData.transformJsonNode()

        val createDate by Table.colCreateDate
    }

    class Bean(entity: Entity) {
        val id = entity.id.value

        val title = entity.title

        val artists = entity.artists.map { ArtistData.Bean(it) }

        val diskNumber = entity.diskNumber
        val trackNumber = entity.trackNumber

        val albumId = entity.albumId?.value

        var recordings : Collection<RecordingData.Bean>? = entity.recordings.map { RecordingData.Bean(it) }
            private set

        var hasRecordings = recordings?.isNotEmpty() ?: false
            private set

        fun hideRecordings() {
            hasRecordings = recordings?.isNotEmpty() ?: false
            recordings = null
        }

        val metaData = entity.metaData

        val createDate = entity.createDate
    }
}
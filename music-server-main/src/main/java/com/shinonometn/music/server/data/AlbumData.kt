package com.shinonometn.music.server.data

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.countBy
import com.shinonometn.koemans.exposed.pagingBy
import com.shinonometn.music.server.commons.LongIdMetaDataTable
import com.shinonometn.music.server.commons.transformJsonNode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.selectAll

object AlbumData {

    fun findByName(string : String) : Bean? {
        return Entity.find { Table.colName eq string }.firstOrNull()?.let { Bean(it) }
    }

    fun findById(id : Long) : Bean? {
        return Entity.findById(id)?.let { Bean(it) }
    }

    fun isAlbumExists(albumId: Long): Boolean {
        return Table.countBy(Table.id) { Table.id eq albumId } > 0
    }

    fun findAll(paging: PageRequest): Page<Bean> {
        return Table.selectAll().pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    object Table : LongIdMetaDataTable("tb_album_data") {
        val colName = varchar("name", 255)
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var name by Table.colName

        var artists by ArtistData.Entity via AlbumArtistData.Table

        var coverArts by CoverArtData.Entity via AlbumArtCoverData.Table

        var metaData: JsonNode by Table.colMetaData.transformJsonNode()
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val name = entity.name

        val artists = entity.artists.map { ArtistData.Bean(it) }

        val coverArts = entity.coverArts.map { CoverArtData.Bean(it) }

        val metaData = entity.metaData
    }
}
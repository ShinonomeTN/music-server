package com.shinonometn.music.server.media.data

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.koemans.exposed.*
import com.shinonometn.music.server.commons.LongIdMetaDataTable
import com.shinonometn.music.server.commons.transformJsonNode
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime

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

    fun deleteById(id: Long): Int {
        return Table.deleteWhere { Table.id eq id }
    }

    fun findAll(paging: PageRequest, sorting: SortRequest): Page<Bean> {
        return Table.selectAll().orderBy(sorting).pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun entityIdOf(albumId: Long): EntityID<Long> {
        return EntityID(albumId, Table)
    }

    object Table : LongIdMetaDataTable("tb_album_data") {
        val colName = varchar("name", 255)
        val colCreateDate = datetime("create_date").clientDefault { LocalDateTime.now() }
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var name by Table.colName
        var artists by ArtistData.Entity via AlbumArtistRelation.Table
        var coverArts by CoverArtData.Entity via AlbumArtCoverRelation.Table
        var metaData: JsonNode by Table.colMetaData.transformJsonNode()
        val createDate by Table.colCreateDate
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val name = entity.name
        val artists = entity.artists.map { ArtistData.Bean(it) }
        val coverArts = entity.coverArts.map { CoverArtData.Bean(it) }
        val metaData = entity.metaData
        val createDate = entity.createDate
    }
}
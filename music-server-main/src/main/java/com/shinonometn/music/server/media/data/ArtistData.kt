package com.shinonometn.music.server.media.data

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
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll

object ArtistData {
    fun findAll(paging: PageRequest): Page<Bean> {
        return Table.selectAll().pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun isArtistsExists(artistIds: List<Long>): Boolean {
        return Table.countBy(Table.id) { Table.id inList artistIds } > 0
    }

    fun findById(id: Long): Bean? {
        return Entity.findById(id)?.let { Bean(it) }
    }

    fun deleteById(id: Long): Int {
        return Table.deleteWhere { Table.id eq id }
    }

    object Table : LongIdMetaDataTable("tb_artist_data") {
        val colName = varchar("name", 255)
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var name by Table.colName
        var coverArts by CoverArtData.Entity via ArtistCoverArtData.Table
        var metaData: JsonNode by Table.colMetaData.transformJsonNode()
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val name = entity.name
        val coverArts = entity.coverArts.map { CoverArtData.Bean(it) }
        val metaData = entity.metaData
    }
}

package com.shinonometn.music.server.data

import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.countBy
import com.shinonometn.koemans.exposed.pagingBy
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object PlaylistData {
    fun findAll(paging: PageRequest): Page<Bean> {
        return Table.selectAll().pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun isPlayListExists(id : Long) : Boolean = Table.countBy(Table.id) {
        Table.id eq id
    } > 0

    fun removeCoverById(id: Long): Int {
        return Table.update({ Table.colCoverArtId eq id }) {
            it[colCoverArtId] = null
        }
    }

    object Table : LongIdTable("tb_playlist") {
        val colCreatorId = long("creator_id")
        val colIsPrivate = bool("is_private")
        val colName = varchar("name", 255)
        val colDescription = text("description").default("")
        val colCoverArtId = long("cover_art_id").nullable()
        val colCreatedAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table) {
            fun findByIdAndOwnerId(playlistId: Long, ownerId: Long): Entity? {
                return find {
                    (Table.id eq playlistId) and (Table.colCreatorId eq ownerId)
                }.firstOrNull()
            }
        }

        var creatorId by Table.colCreatorId
        var isPrivate by Table.colIsPrivate
        var name by Table.colName
        var description by Table.colDescription
        var coverArtId by Table.colCoverArtId
        var coverArt by CoverArtData.Entity optionalReferencedOn Table.colCoverArtId
        var createdAt by Table.colCreatedAt
    }

    class Bean(entity: Entity) {
        val id = entity.id.value
        val creatorId = entity.creatorId
        val isPrivate = entity.isPrivate
        val name = entity.name
        val description = entity.description
        val coverArtId = entity.coverArtId
        val coverArt = entity.coverArt?.let { CoverArtData.Bean(it) }
        val createdAt = entity.createdAt
    }
}
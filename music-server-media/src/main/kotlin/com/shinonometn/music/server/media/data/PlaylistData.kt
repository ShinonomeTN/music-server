package com.shinonometn.music.server.media.data

import com.shinonometn.koemans.exposed.*
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object PlaylistData {
    fun findAll(paging: PageRequest): Page<Bean> {
        return Table.selectAll().pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun isPlayListExists(id: Long): Boolean = Table.countBy(Table.id) {
        Table.id eq id
    } > 0

    fun removeCoverById(id: Long): Int {
        return Table.update({ Table.colCoverArtId eq id }) {
            it[colCoverArtId] = null
        }
    }

    fun findAllByUserId(userId: Long, paging: PageRequest): Page<Bean> = Table
        .select { Table.colCreatorId eq userId }
        .pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }

    fun findById(id: Long): Bean? {
        return Entity.findById(id)?.let { Bean(it) }
    }

    fun isUserOwnPlaylist(userId: Long, id: Long): Boolean {
        return Table.countBy(Table.id) { Table.colCreatorId eq userId and (Table.id eq id) } > 0
    }

    fun findAllPublic(paging: PageRequest, sorting: SortRequest): Page<Bean> {
        return Table.select { Table.colIsPrivate eq false }.orderBy(sorting).pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    object Table : LongIdTable("tb_playlist") {
        val colCreatorId = long("creator_id")
        val colIsPrivate = bool("is_private")
        val colName = varchar("name", 255)
        val colDescription = text("description").default("")
        val colCoverArtId = reference("cover_art_id", CoverArtData.Table.id).nullable()
        val colCreatedAt = datetime("created_at").clientDefault { LocalDateTime.now() }
        val colUpdateAt = datetime("update_at").clientDefault { LocalDateTime.now() }
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
        var updateAt by Table.colUpdateAt
    }

    class Bean(entity: Entity) {
        val id = entity.id.value
        val creatorId = entity.creatorId
        val isPrivate = entity.isPrivate
        val name = entity.name
        val description = entity.description
        val coverArtId = entity.coverArtId?.value
        val coverArt = entity.coverArt?.let { CoverArtData.Bean(it) }
        val createdAt = entity.createdAt
        val updateAt = entity.updateAt
    }
}
package com.shinonometn.music.server.media.data

import com.shinonometn.koemans.exposed.*
import com.shinonometn.music.server.media.data.AlbumData.Table.clientDefault
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime

object RecordingData {
    val sortOptions = SortOptionMapping {
        "create_date" associateTo Table.colCreateDate
    }

    val filterOptions = FilterOptionMapping {
        "protocol" means { Table.colProtocol eq it.asString() }
        "server" means { Table.colServer eq it.asString() }
    }

    fun deleteById(recordingId: Long): Boolean {
        return Table.deleteWhere { Table.id eq recordingId } > 0
    }

    fun findAllByTrackId(id: Long): List<Bean> {
        return Entity.find { Table.colTrackId eq id }.map { Bean(it) }
    }

    fun deleteByTrackId(id: Long): Int {
        return Table.deleteWhere { Table.colTrackId eq id }
    }

    fun findAllByTrackId(id: Long, filtering: FilterRequest, sorting: SortRequest): List<Bean> {
        return Table.selectBy(filtering) { it and (Table.colTrackId eq id ) }
                    .orderBy(sorting)
                    .map { Bean(Entity.wrapRow(it)) }
    }

    object Table : LongIdTable("tb_recording_data") {
        val colTrackId = reference("track_id", TrackData.Table)

        val colProtocol = varchar("protocol", 255)
        val colServer = varchar("server", 255)
        val colLocation = varchar("location", 2048)
        val colCreateDate = datetime("create_date").clientDefault { LocalDateTime.now() }
        val colUpdateDate = datetime("update_date").clientDefault { LocalDateTime.now() }
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var trackId by Table.colTrackId
        var track by TrackData.Entity referencedOn Table.colTrackId
        var protocol by Table.colProtocol
        var server by Table.colServer
        var location by Table.colLocation
        val createDate by Table.colCreateDate
        var updateDate by Table.colUpdateDate
    }

    class Bean(entity: Entity) {
        val id = entity.id.value

        val trackId = entity.trackId.value

        val protocol = entity.protocol
        val server = entity.server
        val location = entity.location

        val fullPath = "${entity.protocol}://${entity.server}/${entity.location}"
        val createDate = entity.createDate
        val updateDate = entity.updateDate
    }

}
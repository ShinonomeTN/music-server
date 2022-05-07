package com.shinonometn.music.server.media.data

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteWhere

object RecordingData {
    fun deleteById(recordingId: Long): Boolean {
        return Table.deleteWhere { Table.id eq recordingId } > 0
    }

    fun findAllByTrackId(id: Long): List<Bean> {
        return Entity.find { Table.colTrackId eq id }.map { Bean(it) }
    }

    fun deleteByTrackId(id: Long): Int {
        return Table.deleteWhere { Table.colTrackId eq id }
    }

    object Table : LongIdTable("tb_recording_data") {
        val colTrackId = reference("track_id", TrackData.Table)

        val colProtocol = varchar("protocol", 255)
        val colServer = varchar("server", 255)
        val colLocation = varchar("location", 2048)
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var trackId by Table.colTrackId
        var track by TrackData.Entity referencedOn Table.colTrackId
        var protocol by Table.colProtocol
        var server by Table.colServer
        var location by Table.colLocation
    }

    class Bean(entity: Entity) {
        val id = entity.id.value

        val trackId = entity.trackId.value

        val protocol = entity.protocol
        val server = entity.server
        val location = entity.location

        val fullPath = "${entity.protocol}://${entity.server}/${entity.location}"
    }

}
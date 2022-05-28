package com.shinonometn.music.server.media.service

import com.shinonometn.koemans.exposed.FilterRequest
import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.media.data.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.springframework.stereotype.Service

@Service
class TrackService(private val database: SqlDatabase) {
    fun findTracksByAlbumId(id: Long): List<TrackData.Bean> = database {
        TrackData.findAllByAlbumId(id)
    }

    fun createTrack(title: String, disNumber: Int?, trackNumber: Int?, albumId: Long?, artistIds: List<Long>): TrackData.Bean? {
        return database {
            val entity = TrackData.Entity.new {
                this.albumId = albumId?.let { EntityID(it, AlbumData.Table) }
                this.title = title
                this.diskNumber = disNumber
                this.trackNumber = trackNumber
            }

            artistIds.forEach { artistId ->
                TrackArtistRelation.Table.insert {
                    it[colTrack] = entity.id
                    it[colArtist] = EntityID(artistId, ArtistData.Table)
                }
            }

            TrackData.Entity.findById(entity.id)?.let { TrackData.Bean(it) }
        }
    }

    fun getTrackById(id: Long): TrackData.Bean? {
        return database { TrackData.getById(id) }
    }

    fun updateTrack(id: Long, title: String, disNumber: Int?, trackNumber: Int?, albumId: Long?, artistIds: List<Long>): TrackData.Bean? {
        return database {
            val entity = TrackData.Entity.findById(id) ?: return@database null
            entity.title = title
            entity.albumId = albumId?.let { EntityID(it, AlbumData.Table) }
            entity.diskNumber = disNumber
            entity.trackNumber = trackNumber

            artistIds.forEach { artistId ->
                TrackArtistRelation.Table.deleteWhere { TrackArtistRelation.Table.colArtist eq artistId }
                TrackArtistRelation.Table.insert {
                    it[colTrack] = entity.id
                    it[colArtist] = EntityID(artistId, ArtistData.Table)
                }
            }

            TrackData.Entity.findById(id)?.let { TrackData.Bean(it) }
        }
    }

    fun listAllTracks(paging: PageRequest, filtering: FilterRequest, sorting: SortRequest): Page<TrackData.Bean> {
        return database {
            TrackData.findAll(paging, filtering, sorting)
        }
    }

    fun deleteTrack(id: Long): Int = database {
        RecordingData.deleteByTrackId(id) +
                TrackArtistRelation.deleteRelationshipsByTrackId(id) +
                TrackData.deleteById(id)
    }
}
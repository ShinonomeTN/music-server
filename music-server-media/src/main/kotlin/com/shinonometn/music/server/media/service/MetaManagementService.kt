package com.shinonometn.music.server.media.service

import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.media.data.*
import com.shinonometn.music.server.media.event.CoverArtDeleteEvent
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class MetaManagementService(private val database: SqlDatabase) {

    private val logger = LoggerFactory.getLogger(MetaManagementService::class.java)

    /* Tracks */

    fun listAllTracks(paging: PageRequest): Page<TrackData.Bean> {
        return database {
            TrackData.listAll(paging)
        }
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
                TrackArtistData.Table.insert {
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
                TrackArtistData.Table.deleteWhere { TrackArtistData.Table.colArtist eq artistId }
                TrackArtistData.Table.insert {
                    it[colTrack] = entity.id
                    it[colArtist] = EntityID(artistId, ArtistData.Table)
                }
            }

            TrackData.Entity.findById(id)?.let { TrackData.Bean(it) }
        }
    }

    /* Recording */

    fun addTrackRecording(trackId: Long, protocol: String, server: String, location: String): RecordingData.Bean {
        return database {
            RecordingData.Bean(RecordingData.Entity.new {
                this.trackId = EntityID(trackId, TrackData.Table)
                this.protocol = protocol
                this.server = server
                this.location = location
            })
        }
    }

    fun updateTrackRecording(id: Long, protocol: String, server: String, location: String): RecordingData.Bean? {
        return database {
            val entity = RecordingData.Entity.findById(id) ?: return@database null
            entity.protocol = protocol
            entity.server = server
            entity.location = location
            RecordingData.Bean(entity)
        }
    }

    fun deleteTrackRecording(recordingId: Long): Boolean {
        return database { RecordingData.deleteById(recordingId) }
    }

    /* Album */

    fun isAlbumExists(albumId: Long): Boolean {
        return database { AlbumData.isAlbumExists(albumId) }
    }

    fun createAlbum(title: String, albumArtIds: List<Long>, albumArtistIds: List<Long>) = database {
        val bean = AlbumData.Bean(AlbumData.Entity.new {
            this.name = title
        })

        albumArtIds.forEach {
            AlbumArtCoverData.createRelation(bean.id, it)
        }

        albumArtistIds.forEach {
            AlbumArtistData.createRelation(bean.id, it)
        }

        bean
    }

    fun updateAlbum(id: Long, title: String, albumArtIds: List<Long>, albumArtistIds: List<Long>) = database {
        val entity = AlbumData.Entity.findById(id) ?: return@database null
        entity.name = title

        AlbumArtCoverData.removeAllRelationsByAlbumId(id)
        AlbumArtistData.removeAllRelationsByAlbumId(id)

        albumArtIds.forEach { AlbumArtCoverData.createRelation(id, it) }
        albumArtistIds.forEach { AlbumArtistData.createRelation(id, it) }

        AlbumData.Bean(entity)
    }

    fun getAlbumById(id: Long) = database {
        AlbumData.Entity.findById(id)?.let { AlbumData.Bean(it) }
    }

    fun findAllAlbums(paging: PageRequest): Page<AlbumData.Bean> {
        return database { AlbumData.findAll(paging) }
    }

    /* Artist */

    fun findAllArtists(paging: PageRequest): Page<ArtistData.Bean> {
        return database { ArtistData.findAll(paging) }
    }

    fun createArtist(name: String, coverArtIds: List<Long>): ArtistData.Bean {
        return database {
            val bean = ArtistData.Bean(ArtistData.Entity.new {
                this.name = name
            })

            coverArtIds.forEach { coverArtId -> ArtistCoverArtData.createRelation(bean.id, coverArtId) }

            bean
        }
    }

    fun updateArtist(id: Long, name: String, coverArtIds: List<Long>): ArtistData.Bean? {
        return database {
            val entity = ArtistData.Entity.findById(id) ?: return@database null
            entity.name = name

            ArtistCoverArtData.removeAllRelationsByArtistId(id)
            coverArtIds.forEach { coverArtId -> ArtistCoverArtData.createRelation(id, coverArtId) }

            ArtistData.Bean(entity)
        }
    }

    fun isArtistsExists(artistIds: List<Long>): Boolean {
        return database { ArtistData.isArtistsExists(artistIds) }
    }

    fun getArtistById(id: Long) = database { ArtistData.findById(id) }

    fun getRecordingsByTrackId(id: Long) = database {
        RecordingData.findAllByTrackId(id)
    }

    fun findAllAlbumTracks(id: Long): List<TrackData.Bean> = database {
        TrackData.findAllByAlbumId(id)
    }

    @EventListener(CoverArtDeleteEvent::class)
    fun onCoverArtDelete(event: CoverArtDeleteEvent) {
        database {
            val clearedAlbumCover = AlbumArtCoverData.removeAllRelationsByCoverId(event.id)
            logger.info("Clear {} album cover associated to {}.", clearedAlbumCover, event.id)
            val clearedArtistCover = ArtistCoverArtData.removeAllRelationsByCoverId(event.id)
            logger.info("Clear {} artist cover associated to {}.", clearedArtistCover, event.id)
        }
    }

    fun deleteAlbum(id: Long) : Int = database {
        AlbumArtCoverData.removeAllRelationsByAlbumId(id) + AlbumData.deleteById(id)
    }

    fun deleteArtist(id: Long): Int = TrackArtistData.deleteByArtistId(id) +
            ArtistCoverArtData.removeAllRelationsByArtistId(id) +
            ArtistData.deleteById(id)

    fun deleteTrack(id: Long): Int = database {
         RecordingData.deleteByTrackId(id) +
                 TrackArtistData.deleteRelationshipsByTrackId(id) +
                 TrackData.deleteById(id)
    }

    fun findArtistsByName(name: String) = database {
        ArtistData.findByName(name)
    }
}
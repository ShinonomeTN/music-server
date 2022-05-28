package com.shinonometn.music.server.media.service

import com.shinonometn.koemans.exposed.*
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


    fun getRecordingsByTrackId(id: Long, filtering: FilterRequest ,sorting: SortRequest): List<RecordingData.Bean> {
        return database {
            RecordingData.findAllByTrackId(id, filtering, sorting)
        }
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
            AlbumArtCoverRelation.createRelation(bean.id, it)
        }

        albumArtistIds.forEach {
            AlbumArtistRelation.createRelation(bean.id, it)
        }

        bean
    }

    fun updateAlbum(id: Long, title: String, albumArtIds: List<Long>, albumArtistIds: List<Long>) = database {
        val entity = AlbumData.Entity.findById(id) ?: return@database null
        entity.name = title

        AlbumArtCoverRelation.removeAllRelationsByAlbumId(id)
        AlbumArtistRelation.removeAllRelationsByAlbumId(id)

        albumArtIds.forEach { AlbumArtCoverRelation.createRelation(id, it) }
        albumArtistIds.forEach { AlbumArtistRelation.createRelation(id, it) }

        AlbumData.Bean(entity)
    }

    fun getAlbumById(id: Long) = database {
        AlbumData.Entity.findById(id)?.let { AlbumData.Bean(it) }
    }

    fun findAllAlbums(paging: PageRequest, sorting : SortRequest) : Page<AlbumData.Bean> {
        return database { AlbumData.findAll(paging, sorting) }
    }

    fun deleteAlbum(id: Long) : Int = database {
        AlbumArtCoverRelation.removeAllRelationsByAlbumId(id) +
                TrackData.removeAlbumRelation(id) +
                AlbumData.deleteById(id)
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

            coverArtIds.forEach { coverArtId -> ArtistCoverArtRelation.createRelation(bean.id, coverArtId) }

            bean
        }
    }

    fun updateArtist(id: Long, name: String, coverArtIds: List<Long>): ArtistData.Bean? {
        return database {
            val entity = ArtistData.Entity.findById(id) ?: return@database null
            entity.name = name

            ArtistCoverArtRelation.removeAllRelationsByArtistId(id)
            coverArtIds.forEach { coverArtId -> ArtistCoverArtRelation.createRelation(id, coverArtId) }

            ArtistData.Bean(entity)
        }
    }


    fun findAllArtists(paging: PageRequest, sorting: SortRequest, filtering: FilterRequest): Page<ArtistData.Bean> {
        return database {
            ArtistData.findAll(paging, sorting, filtering)
        }
    }

    fun deleteArtist(id: Long): Int = TrackArtistRelation.deleteByArtistId(id) +
            ArtistCoverArtRelation.removeAllRelationsByArtistId(id) +
            ArtistData.deleteById(id)

    fun isArtistsExists(artistIds: List<Long>): Boolean {
        return database { ArtistData.isArtistsExists(artistIds) }
    }

    fun getArtistById(id: Long) = database { ArtistData.findById(id) }

    //
    // Event handlers
    //

    @EventListener(CoverArtDeleteEvent::class)
    fun onCoverArtDelete(event: CoverArtDeleteEvent) {
        database {
            val clearedAlbumCover = AlbumArtCoverRelation.removeAllRelationsByCoverId(event.id)
            logger.info("Clear {} album cover associated to {}.", clearedAlbumCover, event.id)
            val clearedArtistCover = ArtistCoverArtRelation.removeAllRelationsByCoverId(event.id)
            logger.info("Clear {} artist cover associated to {}.", clearedArtistCover, event.id)
        }
    }
}
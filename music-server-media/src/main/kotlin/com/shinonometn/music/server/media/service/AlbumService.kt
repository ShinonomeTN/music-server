package com.shinonometn.music.server.media.service

import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.media.data.AlbumArtCoverRelation
import com.shinonometn.music.server.media.data.AlbumArtistRelation
import com.shinonometn.music.server.media.data.AlbumData
import com.shinonometn.music.server.media.data.TrackData
import com.shinonometn.music.server.media.event.CoverArtDeleteEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class AlbumService(private val database: SqlDatabase) {
    private val logger = LoggerFactory.getLogger(AlbumService::class.java)

    fun isAlbumExists(albumId: Long): Boolean {
        return database { AlbumData.isAlbumExists(albumId) }
    }

    fun createAlbum(title: String, albumArtIds: List<Long>, albumArtistIds: List<Long>) = database {
        val bean = AlbumData.Bean(AlbumData.Entity.new { this.name = title })
        albumArtIds.forEach { AlbumArtCoverRelation.createRelation(bean.id, it) }
        albumArtistIds.forEach { AlbumArtistRelation.createRelation(bean.id, it) }
        bean
    }

    fun findAllAlbums(paging: PageRequest, sorting: SortRequest): Page<AlbumData.Bean> {
        return database { AlbumData.findAll(paging, sorting) }
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

    fun deleteAlbumById(id: Long): Int = database {
        AlbumArtCoverRelation.removeAllRelationsByAlbumId(id) +
                TrackData.removeAlbumRelation(id) +
                AlbumData.deleteById(id)
    }

    //
    // Event handling
    //

    @EventListener(CoverArtDeleteEvent::class)
    fun onCoverArtDelete(event: CoverArtDeleteEvent) {
        database {
            val clearedAlbumCover = AlbumArtCoverRelation.removeAllRelationsByCoverId(event.id)
            logger.info("Clear {} album cover associated to {}.", clearedAlbumCover, event.id)
        }
    }
}
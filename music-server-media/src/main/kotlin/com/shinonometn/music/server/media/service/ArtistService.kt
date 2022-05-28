package com.shinonometn.music.server.media.service

import com.shinonometn.koemans.exposed.FilterRequest
import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.media.data.ArtistCoverArtRelation
import com.shinonometn.music.server.media.data.ArtistData
import com.shinonometn.music.server.media.data.TrackArtistRelation
import com.shinonometn.music.server.media.event.CoverArtDeleteEvent
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class ArtistService(private val database: SqlDatabase) {
    private val logger = LoggerFactory.getLogger(ArtistService::class.java)

    fun findAllArtists(paging: PageRequest, sorting: SortRequest, filtering: FilterRequest): Page<ArtistData.Bean> {
        return database {
            ArtistData.findAll(paging, sorting, filtering)
        }
    }

    fun createArtist(name: String, coverArtIds: List<Long>): ArtistData.Bean {
        return database {
            val bean = ArtistData.Bean(ArtistData.Entity.new { this.name = name })
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

    fun deleteArtistById(id: Long): Int = TrackArtistRelation.deleteByArtistId(id) +
            ArtistCoverArtRelation.removeAllRelationsByArtistId(id) +
            ArtistData.deleteById(id)

    fun isArtistsExists(artistIds: List<Long>): Boolean {
        return database { ArtistData.isArtistsExists(artistIds) }
    }

    fun getArtistById(id: Long) = database { ArtistData.findById(id) }

    //
    // Event handling
    //

    @EventListener(CoverArtDeleteEvent::class)
    fun onCoverArtDelete(event: CoverArtDeleteEvent) {
        database {
            val clearedArtistCover = ArtistCoverArtRelation.removeAllRelationsByCoverId(event.id)
            logger.info("Clear {} artist cover associated to {}.", clearedArtistCover, event.id)
        }
    }
}
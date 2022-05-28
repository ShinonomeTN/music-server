package com.shinonometn.music.server.media.service

import com.shinonometn.koemans.exposed.FilterRequest
import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.media.data.*
import com.shinonometn.music.server.media.event.CoverArtDeleteEvent
import org.jetbrains.exposed.dao.id.EntityID
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class PlaylistService(private val database: SqlDatabase) {
    private val logger = LoggerFactory.getLogger(PlaylistService::class.java)

    fun findAll(paging: PageRequest): Page<PlaylistData.Bean> {
        return database {
            PlaylistData.findAll(paging)
        }
    }

    fun create(ownerId: Long, name: String, description: String?, coverArtId: Long?, isPrivate: Boolean) = database {
        PlaylistData.Bean(PlaylistData.Entity.new {
            this.creatorId = ownerId
            this.name = name
            this.description = description ?: ""
            coverArtId?.let { this.coverArtId = EntityID(coverArtId, CoverArtData.Table) }
            this.isPrivate = isPrivate
        })
    }

    fun update(playlistId: Long, ownerId: Long, name: String, description: String?, coverArtId: Long?, private: Boolean): PlaylistData.Bean? =
        database {
            val entity = PlaylistData.Entity.findByIdAndOwnerId(playlistId, ownerId) ?: return@database null
            entity.name = name
            entity.description = description ?: ""
            coverArtId?.let { entity.coverArtId = EntityID(coverArtId, CoverArtData.Table) }
            entity.isPrivate = private
            PlaylistData.Bean(entity)
        }

    fun delete(playlistId: Long, ownerId: Long) = database {
        val entity = PlaylistData.Entity.findByIdAndOwnerId(playlistId, ownerId) ?: return@database false
        entity.delete()
        PlaylistItemData.deleteByPlayListId(playlistId)
        true
    }

    fun addItemsToPlayList(id: Long, trackIds: List<Long>): Boolean {
        if (!database { PlaylistData.isPlayListExists(id) }) return false
        database {
            trackIds.forEach {
                PlaylistItemData.Entity.new {
                    this.playlistId = id
                    this.trackId = EntityID(it, TrackData.Table)
                }
            }
        }
        return true
    }

    fun deleteItemsFromPlaylist(id: Long, itemIds: List<Long>): Boolean {
        if (!database { PlaylistData.isPlayListExists(id) }) return false
        return database {
            itemIds.sumOf {
                PlaylistItemData.deletePlayListItem(id, it)
            }
        } > 0
    }

    fun moveItemAbove(playlistId: Long, itemId: Long, targetItemId: Long): Boolean {
        val current = System.currentTimeMillis()
        val result = database { PlaylistItemData.moveItemAboveOf(playlistId, itemId, targetItemId) }
        val time = System.currentTimeMillis() - current
        logger.info("moveItemAbove took: {} ms", time)
        return result > 0
    }

    fun moveItemBelow(id: Long, itemId: Long, targetItemId: Long): Boolean {
        val current = System.currentTimeMillis()
        val result = database {
            PlaylistItemData.moveItemBelowOf(id, itemId, targetItemId)
        }
        val time = System.currentTimeMillis() - current
        logger.info("moveItemBelow took: {} ms", time)
        return result > 0
    }

    fun findAllPlaylistItem(playlistId: Long, paging: PageRequest, sorting: SortRequest) = database {
        PlaylistItemData.findAll(playlistId, paging, sorting)
    }

    fun findAllByUserId(userId: Long, paging: PageRequest) = database {
        PlaylistData.findAllByUserId(userId, paging)
    }

    fun findById(id: Long) = database {
        PlaylistData.findById(id)
    }

    fun isUserOwnPlaylist(userId: Long, playlistId: Long) = database {
        PlaylistData.isUserOwnPlaylist(userId, playlistId)
    }

    fun findAllPublicPlaylist(paging: PageRequest, filtering: FilterRequest, sorting: SortRequest): Page<PlaylistData.Bean> {
        return database {
            PlaylistData.findAllPublic(paging, filtering, sorting)
        }
    }

    //
    // Event handling
    //
    @EventListener(CoverArtDeleteEvent::class)
    fun onCoverArtDelete(event: CoverArtDeleteEvent) {
        database {
            val clearedPlaylistCover = PlaylistData.removeCoverById(event.id)
            logger.info("Clear {} playlist cover associated to {}.", clearedPlaylistCover, event.id)
        }
    }
}
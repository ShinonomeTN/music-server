package com.shinonometn.music.server.service

import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.data.PlaylistData
import com.shinonometn.music.server.data.PlaylistItemData
import com.shinonometn.music.server.event.CoverArtDeleteEvent
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
            this.coverArtId = coverArtId
            this.isPrivate = isPrivate
        })
    }

    fun update(playlistId: Long, ownerId: Long, name: String, description: String?, coverArtId: Long?, private: Boolean): PlaylistData.Bean? =
        database {
            val entity = PlaylistData.Entity.findByIdAndOwnerId(playlistId, ownerId) ?: return@database null
            entity.name = name
            entity.description = description ?: ""
            entity.coverArtId = coverArtId
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
                    this.trackId = it
                }
            }
        }
        return true
    }

    fun deleteItemsFromPlaylist(id: Long, itemIds: List<Long>): Boolean {
        if (!database { PlaylistData.isPlayListExists(id) }) return false
        return database {
            itemIds.map {
                PlaylistItemData.deletePlayListItem(id, it)
            }.sum()
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

    @EventListener(CoverArtDeleteEvent::class)
    fun onCoverArtDelete(event: CoverArtDeleteEvent) {
        database {
            val clearedPlaylistCover = PlaylistData.removeCoverById(event.id)
            logger.info("Clear {} playlist cover associated to {}.", clearedPlaylistCover, event.id)
        }
    }
}
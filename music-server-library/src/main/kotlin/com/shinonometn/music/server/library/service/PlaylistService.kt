package com.shinonometn.music.server.library.service

import com.shinonometn.koemans.exposed.*
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.library.PlaylistServiceWorker
import com.shinonometn.music.server.library.data.PlaylistData
import com.shinonometn.music.server.library.data.PlaylistItemData
import com.shinonometn.music.server.library.data.UserSubscribedPlaylistData
import com.shinonometn.music.server.media.data.CoverArtData
import com.shinonometn.music.server.media.data.TrackData
import com.shinonometn.music.server.media.event.CoverArtDeleteEvent
import com.shinonometn.music.server.platform.security.data.UserData
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.or
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PlaylistService(private val database: SqlDatabase, private val worker : PlaylistServiceWorker) {
    private val logger = LoggerFactory.getLogger(PlaylistService::class.java)

    //
    // Playlist
    //

    fun create(ownerId: Long, name: String, description: String?, coverArtId: Long?, isPrivate: Boolean) = database {
        PlaylistData.Bean(PlaylistData.Entity.new {
            this.creatorId = UserData.entityIdOf(ownerId)
            this.name = name
            this.description = description ?: ""
            coverArtId?.let { this.coverArtId = CoverArtData.entityIdOf(it) }
            this.isPrivate = isPrivate
        })
    }

    fun update(playlistId: Long, ownerId: Long, name: String, description: String?, coverArtId: Long?, private: Boolean): PlaylistData.Bean? =
        database {
            val entity = PlaylistData.Entity.findByIdAndOwnerId(playlistId, ownerId) ?: return@database null
            entity.name = name
            entity.description = description ?: ""
            coverArtId?.let { entity.coverArtId = CoverArtData.entityIdOf(it) }
            entity.isPrivate = private
            entity.updateAt = LocalDateTime.now()
            PlaylistData.Bean(entity)
        }.also { worker.updatePlaylistMeta(playlistId) }

    fun delete(playlistId: Long, ownerId: Long) = database {
        val entity = PlaylistData.Entity.findByIdAndOwnerId(playlistId, ownerId) ?: return@database false
        entity.delete()
        PlaylistItemData.deleteByPlayListId(playlistId)
        true
    }

    fun findAllPlaylistBy(userId: Long, paging: PageRequest, sorting: SortRequest, filtering: FilterRequest) = database {
        val tbPlaylist = PlaylistData.Table
        val tbUserSubscribe = UserSubscribedPlaylistData.Table

        tbPlaylist.join(tbUserSubscribe, JoinType.LEFT, tbPlaylist.id eq tbUserSubscribe.colPlaylistId){
            (tbUserSubscribe.colUserId eq userId) or (tbPlaylist.colCreatorId eq userId)
        }.selectBy(filtering).orderBy(sorting).pagingBy(paging) {
            val subscribeInfo = if(it.hasValue(tbUserSubscribe.id)) UserSubscribedPlaylistData.Bean(UserSubscribedPlaylistData.Entity.wrapRow(it)) else null
            val playlistInfo = if(it.hasValue(tbPlaylist.id)) PlaylistData.Bean(PlaylistData.Entity.wrapRow(it)) else null
            subscribeInfo to playlistInfo
        }
    }

    fun findAllPublicPlaylist(paging: PageRequest, filtering: FilterRequest, sorting: SortRequest): Page<PlaylistData.Bean> {
        return database {
            PlaylistData.findAllPublic(paging, filtering, sorting)
        }
    }

    fun findById(id: Long) = database { PlaylistData.findById(id) }

    fun isUserOwnPlaylist(userId: Long, playlistId: Long) = database {
        PlaylistData.isUserOwnPlaylist(userId, playlistId)
    }

    //
    // Playlist Item
    //

    fun addItemsToPlayList(id: Long, trackIds: List<Long>): Boolean {
        if (!database { PlaylistData.isPlayListExists(id) }) return false
        database {
            trackIds.forEach {
                PlaylistItemData.Entity.new {
                    this.playlistId = id
                    this.trackId = TrackData.entityIdOf(it)
                }
            }
            PlaylistData.updatePlaylistModifyDate(id)
        }
        worker.updatePlaylistMeta(id)
        return true
    }

    fun deleteItemsFromPlaylist(id: Long, itemIds: List<Long>): Boolean {
        if (!database { PlaylistData.isPlayListExists(id) }) return false
        return (database {
            itemIds.sumOf { PlaylistItemData.deletePlayListItem(id, it) }
            + PlaylistData.updatePlaylistModifyDate(id)
        } > 0).also { worker.updatePlaylistMeta(id) }
    }

    fun moveItemAbove(id: Long, itemId: Long, targetItemId: Long): Boolean {
        val current = System.currentTimeMillis()
        val result = database { PlaylistItemData.moveItemAboveOf(id, itemId, targetItemId) }
        val time = System.currentTimeMillis() - current
        logger.debug("PlaylistService::moveItemAbove({}) took: {} ms", id, time)
        return result > 0
    }

    fun moveItemBelow(id: Long, itemId: Long, targetItemId: Long): Boolean {
        val current = System.currentTimeMillis()
        val result = database {
            PlaylistItemData.moveItemBelowOf(id, itemId, targetItemId)
        }
        val time = System.currentTimeMillis() - current
        logger.debug("PlaylistService::moveItemBelow({}) took: {} ms", id, time)
        return result > 0
    }

    fun findAllPlaylistItem(playlistId: Long, paging: PageRequest, sorting: SortRequest) = database {
        PlaylistItemData.findAll(playlistId, paging, sorting)
    }

    //
    // Subscription
    //
    fun subscribePlaylist(userId: Long, playlistId: Long): Int = database {
        if(UserSubscribedPlaylistData.existsByUserIdAndPlaylistId(userId, playlistId)) 0
        else UserSubscribedPlaylistData.create(userId, playlistId)
    }

    fun findSubscription(userId: Long, playlistId: Long): UserSubscribedPlaylistData.Bean? = database {
        UserSubscribedPlaylistData.findByUserIdAndPlaylistId(userId, playlistId)
    }

    fun unsubscribePlaylist(userId: Long, playlistId: Long): Int = database {
        UserSubscribedPlaylistData.deleteBy(userId, playlistId)
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
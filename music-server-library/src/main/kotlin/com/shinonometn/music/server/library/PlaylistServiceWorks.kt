package com.shinonometn.music.server.library

import com.fasterxml.jackson.databind.ObjectMapper
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.library.bean.PlaylistMetaBean
import com.shinonometn.music.server.library.data.PlaylistData
import com.shinonometn.music.server.library.data.PlaylistItemData
import com.shinonometn.music.server.media.data.ArtistData
import com.shinonometn.music.server.media.data.TrackArtistRelation
import com.shinonometn.music.server.platform.worker.DebounceWorker
import kotlinx.coroutines.Job
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.select
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PlaylistServiceWorker(private val database: SqlDatabase, private val json: ObjectMapper) : DebounceWorker() {
    private val logger = LoggerFactory.getLogger(PlaylistServiceWorker::class.java)

    fun updatePlaylistMeta(playlistId: Long): Job? {
        val key = "updatePlaylistMeta:${playlistId}"
        return tryPushNewJob(key) {
            val playlist = database { PlaylistData.Entity.findById(playlistId) } ?: return@tryPushNewJob
            val tbPlaylistItem = PlaylistItemData.Table
            val tbTrackArtists = TrackArtistRelation.Table
            val artistIds = database {
                tbPlaylistItem.join(tbTrackArtists, JoinType.LEFT, tbTrackArtists.colTrack eq tbPlaylistItem.colTrackId).select {
                    tbPlaylistItem.colPlayListId eq playlistId
                }.distinctBy { tbTrackArtists.colArtist }.map { it[tbTrackArtists.colArtist].value }
            }
            val tbArtist = ArtistData.Table
            val artistInfo = database { tbArtist.select { tbArtist.id inList artistIds }.associate { it[tbArtist.id].value to it[tbArtist.colName] } }
            val meta = PlaylistMetaBean(artistInfo)
            database { playlist.meta = json.valueToTree(meta) }
            logger.info("Update meta of playlist({}) success.", playlistId)
        }
    }
}
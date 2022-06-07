package com.shinonometn.music.server.library.service

import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.library.data.UserFavoriteTrackData
import org.springframework.stereotype.Service

@Service
class UserTrackService(private val database : SqlDatabase) {
    fun findTracksBy(userId: Long, sorting: SortRequest, paging: PageRequest) = database {
        UserFavoriteTrackData.findByUserId(userId, sorting, paging)
    }

    fun favouriteTrack(userId: Long, trackId: Long) = database {
        UserFavoriteTrackData.createRelationship(userId, trackId)
    }

    fun unfavouredTrack(userId: Long, trackId: Long) = database {
        UserFavoriteTrackData.removeRelationship(userId, trackId)
    }

}
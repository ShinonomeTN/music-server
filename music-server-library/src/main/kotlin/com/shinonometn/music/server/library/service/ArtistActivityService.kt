package com.shinonometn.music.server.library.service

import com.shinonometn.koemans.exposed.*
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.library.data.UserFollowedArtistData
import com.shinonometn.music.server.media.data.ArtistData
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ArtistActivityService(private val database: SqlDatabase) {
    fun followArtist(artistId: Long, userId: Long): Int = database {
        UserFollowedArtistData.createRelationship(artistId, userId)
    }

    fun unfollowArtist(artistId: Long, userId: Long) = database {
        UserFollowedArtistData.deleteRelationship(artistId, userId)
    }

    fun fetchArtistUpdate(userId: Long, filtering: FilterRequest, paging: PageRequest) = database {
        val tbUserFollowingArtists = UserFollowedArtistData.Table
        val tbArtist = ArtistData.Table

        tbUserFollowingArtists.join(tbArtist, JoinType.LEFT, tbUserFollowingArtists.colArtistId eq tbArtist.id ){
            (tbUserFollowingArtists.colUserId eq userId) and (tbArtist.colUpdateDate greaterEq tbUserFollowingArtists.colUpdateDate)
        }.selectBy(filtering).orderBy(tbArtist.colUpdateDate, SortOrder.DESC).pagingBy(paging) {
            UserFollowedArtistData.Bean(UserFollowedArtistData.Entity.wrapRow(it))
        }
    }

    fun commitUserArtistUpdateFetched(userId: Long, artistId: Long, datetime: LocalDateTime) = database {
        UserFollowedArtistData.updateFetchTime(userId, artistId, datetime)
    }

    fun fetchArtistUpdateInfo(userId: Long, artistId: Long) = database {
        UserFollowedArtistData.artistUpdateFor(userId, artistId)
    }

    fun findAllBy(userId: Long, sorting: SortRequest, paging: PageRequest) = database {
        UserFollowedArtistData.findAllByUserId(userId, sorting, paging)
    }

}
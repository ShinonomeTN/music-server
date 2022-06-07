package com.shinonometn.music.server.library.service

import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.library.data.UserFavouriteAlbumData
import org.springframework.stereotype.Service

@Service
class UserAlbumService(private val database: SqlDatabase) {
    fun listUserAlbums(userId: Long, sorting: SortRequest, paging: PageRequest) = database {
        UserFavouriteAlbumData.findByUserId(userId, sorting, paging)
    }

    fun favouriteAlbum(userId: Long, albumId: Long) = database {
        UserFavouriteAlbumData.createRelationship(userId, albumId)
    }

    fun unfavouredAlbum(userId: Long, albumId: Long) = database {
        UserFavouriteAlbumData.removeRelationship(userId, albumId)
    }

    fun albumFavouredInfo(userId: Long, albumId: Long): UserFavouriteAlbumData.Bean? {
        return UserFavouriteAlbumData.findByUserIdAndAlbumId(userId, albumId)
    }
}
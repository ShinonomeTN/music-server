package com.shinonometn.music.server.library.api

import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.library.LibraryScope
import com.shinonometn.music.server.library.data.UserFavouriteAlbumData
import com.shinonometn.music.server.library.service.UserAlbumService
import com.shinonometn.music.server.platform.security.commons.acUserIdentityNotNull
import com.shinonometn.music.server.platform.security.commons.accessControl
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/library")
class UserAlbumApi(private val service : UserAlbumService) {

    @KtorRoute("/album")
    fun Route.listUserAlbum() = accessControl(LibraryScope.Album) {
        get {
            val userId = call.acUserIdentityNotNull.userId
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(UserFavouriteAlbumData.sortMapping)

            call.respond(service.listUserAlbums(userId, sorting, paging).convert {
                mapOf("favourite" to it, "album" to it.album)
            })
        }
    }

    @KtorRoute
    fun Route.albumFavourite() = route("/album/{id}") {

        accessControl(LibraryScope.Album) {
            get {
                val albumId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_album_id")
                val userId = call.acUserIdentityNotNull.userId
                val result = service.albumFavouredInfo(userId, albumId)
                call.respond(mapOf(
                    "favourite" to result,
                    "album" to result?.album
                ))
            }
        }

        param("favourite") {
            accessControl(LibraryScope.AlbumFavourite) {
                post {
                    val albumId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_album_id")
                    val userId = call.acUserIdentityNotNull.userId
                    call.respond(CR.successOrFailed(service.favouriteAlbum(userId, albumId)))
                }
            }
        }

        param("unfavoured") {
            accessControl(LibraryScope.AlbumFavourite) {
                post {
                    val albumId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_album_id")
                    val userId = call.acUserIdentityNotNull.userId
                    call.respond(CR.successOrFailed(service.unfavouredAlbum(userId, albumId)))
                }
            }
        }
    }
}
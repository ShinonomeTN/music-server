package com.shinonometn.music.server.library.api

import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.library.LibraryScope
import com.shinonometn.music.server.library.data.UserFavoriteTrackData
import com.shinonometn.music.server.library.service.UserTrackService
import com.shinonometn.music.server.platform.security.commons.acUserIdentityNotNull
import com.shinonometn.music.server.platform.security.commons.accessControl
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/library")
class UserTrackApi(private val service : UserTrackService) {

    @KtorRoute("/track")
    fun Route.listAllTrack() = accessControl(LibraryScope.Track) {
        get {
            val userId = call.acUserIdentityNotNull.userId
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(UserFavoriteTrackData.sortMapping)

            call.respond(service.findTracksBy(userId, sorting, paging).convert {
                mapOf(
                    "favourite" to it,
                    "track" to it.track
                )
            })
        }
    }

    @KtorRoute("/track")
    fun Route.trackFavourite() = route("/{id}") {
        param("favourite") {
            accessControl(LibraryScope.TrackFavourite) {
                post {
                    val trackId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_track_id")
                    val userId = call.acUserIdentityNotNull.userId
                    call.respond(CR.successOrFailed(service.favouriteTrack(userId, trackId)))
                }
            }
        }

        param("unfavoured") {
            accessControl(LibraryScope.TrackUnfavoured) {
                post {
                    val trackId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_track_id")
                    val userId = call.acUserIdentityNotNull.userId
                    call.respond(CR.successOrFailed(service.unfavouredTrack(userId, trackId)))
                }
            }
        }
    }
}
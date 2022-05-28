package com.shinonometn.music.server.media.api.admin

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.utils.isNumber
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.commons.vararg
import com.shinonometn.music.server.media.MediaScope
import com.shinonometn.music.server.media.service.AlbumService
import com.shinonometn.music.server.media.service.ArtistService
import com.shinonometn.music.server.media.service.TrackService
import com.shinonometn.music.server.platform.security.commons.accessControl
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta/track")
class TrackManagementApi(
    private val trackService: TrackService,
    private val albumService: AlbumService,
    private val artistService: ArtistService
) {
    /* Track */

    class TrackInfoForm(params: Parameters) {
        init {
            validator.validate(params)
        }

        val title = params["title"]!!
        val albumId = params["albumId"]?.toLongOrNull()
        val artistIds = params.getAll("artistId")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        val diskNumber = params["diskNumber"]?.toIntOrNull()
        val trackNumber = params["trackNumber"]?.toIntOrNull()

        companion object {
            val validator = Validator(Validator.Policy.Vararg) {
                "title" with listOf(
                    isString("title_should_not_blank") { it.isNotBlank() },
                    isString("title_should_in_255_char") { it.length <= 255 }
                )

                optional("diskNumber") with isString { it.isNumber() }
                optional("trackNumber") with isString { it.isNumber() }

                optional("albumId") with isString { it.isNumber() }
                optional("artistId") with vararg { it.isNumber() }
            }
        }
    }

    @KtorRoute
    fun Route.trackApis() = accessControl(MediaScope.Admin.TrackManagement) {
        post {
            val request = TrackInfoForm(call.receiveParameters())
            if (request.albumId != null && !albumService.isAlbumExists(request.albumId)) businessError("album_not_exists")
            if (request.artistIds.isNotEmpty() && !artistService.isArtistsExists(request.artistIds)) businessError("artist_not_exists")
            val result = background {
                trackService.createTrack(
                    request.title, request.diskNumber, request.trackNumber, request.albumId, request.artistIds
                )
            }
            call.respond(mapOf("track" to result))
        }

        route("/{id}") {
            post {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val request = TrackInfoForm(call.receiveParameters())
                if (request.albumId != null && !albumService.isAlbumExists(request.albumId)) businessError("album_not_exists")
                val result = background {
                    trackService.updateTrack(
                        id, request.title, request.diskNumber, request.trackNumber, request.albumId, request.artistIds
                    )
                }
                call.respond(mapOf("track" to result))
            }

            delete {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                call.respond(CR.successOrFailed(background {
                    trackService.deleteTrack(id)
                }))
            }
        }
    }
}
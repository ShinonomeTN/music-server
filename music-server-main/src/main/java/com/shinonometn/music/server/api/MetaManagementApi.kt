package com.shinonometn.music.server.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.utils.CommonRegex
import com.shinonometn.koemans.utils.isNumber
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.Server
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.commons.vararg
import com.shinonometn.music.server.service.MetaManagementService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta")
class MetaManagementApi(private val service: MetaManagementService) {

    /* Track */

    @KtorRoute("/track")
    fun Route.listTracks() = get {
        val paging = call.receivePageRequest()
        val tracks = background {
            service.listAllTracks(paging).convert {
                mapOf("track" to it)
            }
        }

        call.respond(tracks)
    }

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

    @KtorRoute("/track")
    fun Route.createTrack() = post {
        val request = TrackInfoForm(call.receiveParameters())
        if (request.albumId != null && !service.isAlbumExists(request.albumId)) businessError("album_not_exists")
        if (request.artistIds.isNotEmpty() && !service.isArtistsExists(request.artistIds)) businessError("artist_not_exists")
        val result = background {
            service.createTrack(
                request.title, request.diskNumber, request.trackNumber, request.albumId, request.artistIds
            )
        }
        call.respond(mapOf("track" to result))
    }

    @KtorRoute("/track/{id}")
    fun Route.getTrack() = get {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val result = background { service.getTrackById(id) }
        call.respond(mapOf("track" to result))
    }

    @KtorRoute("/track/{id}")
    fun Route.updateTrack() = post {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val request = TrackInfoForm(call.receiveParameters())
        if (request.albumId != null && !service.isAlbumExists(request.albumId)) businessError("album_not_exists")
        val result = background {
            service.updateTrack(
                id, request.title, request.diskNumber, request.trackNumber, request.albumId, request.artistIds
            )
        }
        call.respond(mapOf("track" to result))
    }

    /* Recording */

    @KtorRoute("/track/{id}/recording")
    fun Route.getRecordings() = get {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val result = background { service.getRecordingsByTrackId(id) }.map {
            mapOf("recording" to it)
        }
        call.respond(result)
    }

    class CreateRecordingRequest(parameters: Parameters) {
        init {
            validator.validate(parameters)
        }

        val protocol = parameters["protocol"]!!
        val server = parameters["server"]!!
        val location = parameters["location"]!!

        companion object {
            val validator = Validator {
                "protocol" with isString { it.isNotBlank() && it.length <= 255 }
                "server" with isString { it.isNotBlank() && it.length <= 255 }
                "location" with isString { it.isNotBlank() }
            }
        }
    }

    @KtorRoute("/track/{id}/recording")
    fun Route.addTrackRecording() = post {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val request = CreateRecordingRequest(call.receiveParameters())
        val result = background { service.addTrackRecording(id, request.protocol, request.server, request.location) }
        call.respond(mapOf("recording" to result))
    }

    @KtorRoute("/recording/{id}")
    fun Route.updateTrackRecord() = post {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val request = CreateRecordingRequest(call.receiveParameters())
        val result = background { service.updateTrackRecording(id, request.protocol, request.server, request.location) }
        call.respond(mapOf("recording" to result))
    }

    @KtorRoute("/recording/{recordingId}")
    fun Route.deleteTrackRecording() = delete {
        val recordingId = call.parameters["recordingId"]?.toLongOrNull() ?: businessError("recordingId_should_be_number")
        val result = background { service.deleteTrackRecording(recordingId) }
        call.respond(CR.successOrFailed(result))
    }

    /* Album */

    @KtorRoute("/album")
    fun Route.getAlbums() = get {
        val paging = call.receivePageRequest()
        val result = background { service.findAllAlbums(paging).convert { mapOf("album" to it) } }
        call.respond(result)
    }

    class AlbumCreateRequest(param: Parameters) {

        init {
            validator.validate(param)
        }

        val title: String = param["title"]!!
        val albumArtIds = param.getAll("albumArtId")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        val albumArtistIds = param.getAll("albumArtistId")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

        companion object {
            val validator = Validator(Validator.Policy.Vararg) {
                "title" with listOf(
                    isString("title_should_not_be_blank") { it.isNotBlank() },
                    isString("length_should_in_255_char") { it.length <= 255 }
                )

                optional("albumArtId") with vararg { it.isNumber() }

                optional("albumArtistId") with vararg { it.isNumber() }
            }
        }
    }

    @KtorRoute("/album")
    fun Route.createAlbum() = post {
        val request = AlbumCreateRequest(call.receiveParameters())
        val result = background { service.createAlbum(request.title, request.albumArtIds, request.albumArtistIds) }
        call.respond(mapOf("album" to result))
    }

    @KtorRoute("/album/{id}")
    fun Route.getAlbum() = get {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val result = background { service.getAlbumById(id) }
        call.respond(mapOf("album" to result))
    }

    @KtorRoute("/album/{id}")
    fun Route.updateAlbum() = post {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val request = AlbumCreateRequest(call.receiveParameters())
        val result = background { service.updateAlbum(id, request.title, request.albumArtIds, request.albumArtistIds) }
        call.respond(mapOf("album" to result))
    }

    @KtorRoute("/album/{id}/track")
    fun Route.getAlbumTracks() = get {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val result = background { service.findAllAlbumTracks(id).map { mapOf("track" to it) } }
        call.respond(result)
    }

    /* Artist */

    @KtorRoute("/artist")
    fun Route.getArtists() = get {
        val paging = call.receivePageRequest()
        val result = background { service.findAllArtists(paging).convert { mapOf("artist" to it) } }
        call.respond(result)
    }

    @KtorRoute("/artist/{id}")
    fun Route.getArtist() = get {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val result = background { service.getArtistById(id) }
        call.respond(mapOf("artist" to result))
    }

    class CreateArtistRequest(params: Parameters) {
        init {
            validator.validate(params)
        }

        val name = params["name"]!!
        val coverArtIds = params.getAll("coverArtIds")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

        companion object {
            val validator = Validator(Validator.Policy.Vararg) {
                "name" with listOf(
                    isString("name_should_in_255_char") { it.length <= 255 },
                    isString("name_should_not_be_blank") { it.isNotBlank() },
                )

                optional("coverArtId") with vararg { it.isNumber() }
            }
        }
    }

    @KtorRoute("/artist")
    fun Route.createArtist() = post {
        val request = CreateArtistRequest(call.receiveParameters())
        val artist = background { service.createArtist(request.name, request.coverArtIds) }
        call.respond(mapOf("artist" to artist))
    }

    @KtorRoute("/artist/{id}")
    fun Route.updateArtist() = post {
        val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
        val request = CreateArtistRequest(call.receiveParameters())
        val artist = background { service.updateArtist(id, request.name, request.coverArtIds) }
        call.respond(mapOf("artist" to artist))
    }
}
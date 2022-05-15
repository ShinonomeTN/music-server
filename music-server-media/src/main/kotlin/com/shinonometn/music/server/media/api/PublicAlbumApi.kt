package com.shinonometn.music.server.media.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.media.service.MetaManagementService
import com.shinonometn.music.server.platform.security.commons.AC
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta/album")
class PublicAlbumApi(private val service: MetaManagementService) {

    @KtorRoute
    fun Route.albumApi() = accessControl(AC.Guest) {
        /** @restful_api_doc
         * # Get all albums
         * [GET] /api/meta/album
         * ## Parameters
         * - @bean(Pagination)
         * ## Returns
         * @bean(Page) of @bean(AlbumData.Bean)
         * ```
         * {..., content: [{ album : @bean(AlbumData.Bean) }]}
         * ```
         */
        get {
            val paging = call.receivePageRequest()
            val result = background { service.findAllAlbums(paging).convert { mapOf("album" to it) } }
            call.respond(result)
        }

        route("/{id}") {
            /** @restful_api_doc
             * # Get album info
             * [GET] /api/meta/album/{id}
             * ## Parameters
             * - id : album id
             * ## Returns
             * @bean(AlbumData.Bean)
             * ```
             * { album : @bean(AlbumData.Bean) }
             * ```
             */
            get {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val result = background { service.getAlbumById(id) }
                call.respond(mapOf("album" to result))
            }

            /** @restful_api_doc
             * # Get album tracks
             * [GET] /api/meta/album/{id}/track
             * ## Parameters
             * - id : album id
             * ## Returns
             * List of @bean(TrackData.Bean)
             * ```
             * [ { track: @bean(TrackData.Bean) } ]
             * ```
             */
            route("/track") {
                get {
                    val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                    val result = background { service.findAllAlbumTracks(id).map { mapOf("track" to it) } }
                    call.respond(result)
                }
            }
        }
    }
}
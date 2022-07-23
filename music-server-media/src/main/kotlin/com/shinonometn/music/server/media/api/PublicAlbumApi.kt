package com.shinonometn.music.server.media.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.exposed.SortOptionMapping
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.media.data.AlbumData
import com.shinonometn.music.server.media.service.AlbumService
import com.shinonometn.music.server.media.service.TrackService
import com.shinonometn.music.server.platform.security.commons.AC
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta/album")
class PublicAlbumApi(private val trackService: TrackService, private val albumService: AlbumService) {

    // TODO: Album webpage and preview

    @KtorRoute
    fun Route.albumApi() = accessControl(AC.IsGuestAllowed) {
        /** @restful_api_doc
         * # Get all albums
         * [GET] /api/meta/album
         * ## Parameters
         * - @bean(Pagination)
         * - Sort options
         * ## Sort options
         * - create_date: Album data create date
         * ## Returns
         * @bean(Page) of @bean(AlbumData.Bean)
         * ```
         * {..., content: [{ album : @bean(AlbumData.Bean) }]}
         * ```
         */
        val albumSorting = SortOptionMapping {
            "create_date" associateTo AlbumData.Table.colCreateDate
        }
        get {
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(albumSorting)
            val result = background { albumService.findAllAlbums(paging, sorting).convert { mapOf("album" to it) } }
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
                val result = background { albumService.getAlbumById(id) }
                call.respond(mapOf("album" to result))
            }

            /** @restful_api_doc
             * # Get album tracks
             * [GET] /api/meta/album/{id}/track
             * For advance track finding, please use @api([GET] /api/meta/track)
             * ## Parameters
             * - id : album id
             * ## Returns
             * List of @bean(TrackData.Bean), sorted by disk id and track id
             * ```
             * [ { track: @bean(TrackData.Bean) } ]
             * ```
             */
            route("/track") {
                get {
                    val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                    val result = background { trackService.findTracksByAlbumId(id).map { mapOf("track" to it) } }
                    call.respond(result)
                }
            }
        }
    }
}
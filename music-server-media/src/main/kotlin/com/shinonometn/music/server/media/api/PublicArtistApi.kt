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
@KtorRoute("/api/meta/artist")
class PublicArtistApi(private val service: MetaManagementService) {

    @KtorRoute
    fun Route.artistApi() = accessControl(AC.Guest) {
        /** @restful_api_doc
         * # Get all artist
         * [GET] /api/meta/artists
         * ## Parameters
         * - @bean(Pagination)
         * ## Returns
         * @bean(Page) of @bean(ArtistData.Bean)
         * ```
         * { ..., content: [{ artist : @bean(ArtistData.Bean) }] }
         * ```
         */
        get {
            val paging = call.receivePageRequest()
            val result = background { service.findAllArtists(paging).convert { mapOf("artist" to it) } }
            call.respond(result)
        }

        /** @restful_api_doc
         * # Get artist info
         * [GET] /api/meta/artists
         * ## Parameters
         * - id : artist id
         * ## Body
         *
         * ## Returns
         * @bean(ArtistData.Bean)
         * ```
         * { artist: @bean(ArtistData.Bean) }
         * ```
         */
        route("/{id}") {
            get {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val result = background { service.getArtistById(id) }
                call.respond(mapOf("artist" to result))
            }
        }
    }

}
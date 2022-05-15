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
@KtorRoute("/api/meta/track")
class PublicTrackApi(private val service: MetaManagementService) {

    @KtorRoute
    fun Route.trackApi() = accessControl(AC.Guest) {
        /** @restful_api_doc
         * # Get all tracks
         * [GET] /api/meta/track
         * ## Parameters
         * - @bean(Pagination)
         * ## Returns
         * @bean(Page) of @bean(TrackData.Bean)
         * ```
         * { ..., content: [{ track: { @bean(TrackData.Bean) } }]}
         * ```
         */
        get {
            val paging = call.receivePageRequest()
            val tracks = background {
                service.listAllTracks(paging).convert {
                    mapOf("track" to it)
                }
            }

            call.respond(tracks)
        }

        route("/{id}") {
            /** @restful_api_doc
             * # Get single track
             * [GET] /api/meta/track/{id}
             * ## Parameters
             * - id : track id
             * ## Returns
             * @bean(TrackData.Bean)
             * ```
             * { track: { @bean(TrackData.Bean) } }
             * ```
             */
            get {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val result = background { service.getTrackById(id) }
                call.respond(mapOf("track" to result))
            }
        }
    }
}
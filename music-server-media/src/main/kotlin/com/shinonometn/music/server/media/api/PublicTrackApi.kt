package com.shinonometn.music.server.media.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.receiveFilterOptions
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.media.data.TrackData
import com.shinonometn.music.server.media.service.MetaManagementService
import com.shinonometn.music.server.media.service.TrackService
import com.shinonometn.music.server.platform.security.commons.AC
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta/track")
class PublicTrackApi(private val trackService: TrackService) {

    @KtorRoute
    fun Route.trackApi() = accessControl(AC.Guest) {
        /** @restful_api_doc
         * # Get all tracks
         * [GET] /api/meta/track
         * ## Parameters
         * - @bean(Pagination)
         * - Sort options
         * - Filter options
         * ## Sort options
         * - create_date: sort by create date
         * - disk_number: sort by disk number
         * - track_number: sort by track number
         * ## Filter options
         * - album_id: filter by album id
         * - title: filter by title
         * - disk_number : filter by disk number
         * - track_number: filter by track number
         * ## Returns
         * @bean(Page) of @bean(TrackData.Bean)
         * ```
         * { ..., content: [{ track: @bean(TrackData.Bean) }]}
         * ```
         */
        get {
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(TrackData.sortOptions)
            val filtering = call.receiveFilterOptions(TrackData.filteringOptions)
            val tracks = background {
                trackService.listAllTracks(paging, filtering, sorting).convert {
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
             * { track: @bean(TrackData.Bean) }
             * ```
             */
            get {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val result = background { trackService.getTrackById(id) }
                call.respond(mapOf("track" to result))
            }
        }
    }
}
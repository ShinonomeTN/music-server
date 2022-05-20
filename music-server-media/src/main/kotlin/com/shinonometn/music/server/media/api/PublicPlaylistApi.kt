package com.shinonometn.music.server.media.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.exposed.SortOptionMapping
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.media.data.PlaylistData
import com.shinonometn.music.server.media.service.PlaylistService
import com.shinonometn.music.server.platform.security.commons.AC
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/playlist")
class PublicPlaylistApi(private val playlistService: PlaylistService) {

    @KtorRoute
    fun Route.getPublicPlaylist() = accessControl(AC.Guest) {
        param("public") {
            /** @restful_api_doc
             * # Get all public playlist
             * [GET] /api/playlist?public
             * ## Parameters
             * - @bean(Pagination)
             * - Sorting options
             * ## Sorting options
             * - create_date: sort by create date, default is desc
             * - update_date: sort by update date, default is desc
             * ## Returns
             * @bean(Page) of @bean(PlaylistData.Bean)
             * ```
             * { ..., content: [{ playlist: @bean(PlaylistData.Bean) }]}
             * ```
             */
            val sort = SortOptionMapping {
                "create_date" associateTo PlaylistData.Table.colCreatedAt defaultOrder SortOrder.DESC
                "update_date" associateTo PlaylistData.Table.colUpdateAt defaultOrder SortOrder.DESC
            }
            get {
                val paging = call.receivePageRequest()
                val sorting = call.receiveSortOptions(sort)
                val result = background {
                    playlistService.findAllPublicPlaylist(paging, sorting).convert {
                        mapOf("playlist" to it)
                    }
                }
                call.respond(result)
            }
        }
    }
}
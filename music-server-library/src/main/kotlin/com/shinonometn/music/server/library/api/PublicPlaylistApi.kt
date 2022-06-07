package com.shinonometn.music.server.library.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.receiveFilterOptions
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.Jackson
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.library.data.PlaylistData
import com.shinonometn.music.server.library.data.PlaylistItemData
import com.shinonometn.music.server.library.service.PlaylistService
import com.shinonometn.music.server.platform.security.commons.AC
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
class PublicPlaylistApi(private val playlistService: PlaylistService) {

    @KtorRoute("/api/playlist/{id}")
    fun Route.getPlaylist() = accessControl(AC.Guest) {
        get {
            val playlistId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_playlist_id")
            val playlist = playlistService.findById(playlistId) ?: CR.Error.notFound("playlist_not_found:${playlistId}")
            if(playlist.isPrivate) CR.Error.forbidden("playlist_is_private:${playlistId}")
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(PlaylistItemData.sortOptions)
            val playlistItemPage = playlistService.findAllPlaylistItem(playlistId,paging,sorting)
            val map = mapOf(
                "playlist" to playlist,
                "itemPage" to playlistItemPage
            )

            call.respond(FreeMarkerContent("playlist.ftl", mapOf(
                "model" to map,
                "modelJson" to Jackson.mapper.writeValueAsString(map)
            )))
        }

        // TODO: Playlist webpage preview
//        get("/preview") {
//
//        }
    }

    @KtorRoute("/api/playlist")
    fun Route.getPublicPlaylist() = accessControl(AC.Guest) {
        param("public") {
            /** @restful_api_doc
             * # Get all public playlist
             * [GET] /api/playlist?public
             * ## Parameters
             * - @bean(Pagination)
             * - Sorting options
             * - Filter options
             * ## Filter options
             * - creator_id: filter by creator id
             * - name : filter by name
             * - created_after: filter by create after datetime
             * - created_before: filter by create before datetime
             * - updated_after: filter by updated after datetime
             * - updated_before: filter by updated before datetime
             * ## Sorting options
             * - create_date: sort by create date
             * - update_date: sort by update date
             * ## Returns
             * @bean(Page) of @bean(PlaylistData.Bean)
             * ```
             * { ..., content: [{ playlist: @bean(PlaylistData.Bean) }]}
             * ```
             */
            val filterOptions = PlaylistData.filterOptions.copy {
                exclude("is_private")
            }
            get {
                val paging = call.receivePageRequest()
                val sorting = call.receiveSortOptions(PlaylistData.sortingOptions)
                val filtering = call.receiveFilterOptions(filterOptions)
                val result = background {
                    playlistService.findAllPublicPlaylist(paging, filtering, sorting).convert {
                        mapOf("playlist" to it)
                    }
                }
                call.respond(result)
            }
        }
    }
}
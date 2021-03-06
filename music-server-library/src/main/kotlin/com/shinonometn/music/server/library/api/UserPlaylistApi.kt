package com.shinonometn.music.server.library.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.receiveFilterOptions
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.utils.isBoolean
import com.shinonometn.koemans.utils.isNumber
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.library.LibraryScope
import com.shinonometn.music.server.library.data.PlaylistData
import com.shinonometn.music.server.library.data.PlaylistItemData
import com.shinonometn.music.server.library.service.PlaylistService
import com.shinonometn.music.server.platform.security.commons.*
import com.shinonometn.music.server.platform.security.service.UserService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/library/playlist")
class UserPlaylistApi(private val playlistService: PlaylistService, private val userService: UserService) {

    @KtorRoute
    fun Route.listPlaylists() = accessControl(LibraryScope.PlaylistRead) {
        /** @restful_api_doc
         * # Get playlist
         * [GET] /api/playlist
         * ## Parameters
         * - @bean(Page)
         * - Sort options
         * ## Sort options
         * - create_date: sort by create data
         * - update_date: sort by update date
         * ## Returns
         * @bean(Page) with @bean(PlaylistData.Bean)
         * ```
         * { ..., content : [{ playlist: @bean(PlaylistData.Bean) }]}
         * ```
         */
        // TODO: Support sort by subscribe date
        get {
            val identity = call.acUserIdentityNotNull
            val userId = identity.userId
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(PlaylistData.sortingOptions)
            val filtering = call.receiveFilterOptions(PlaylistData.filterOptions)

            call.respond(background {
                playlistService.findAllPlaylistBy(userId, paging, sorting, filtering).convert {
                    mapOf(
                        "playlist" to it.second,
                        "subscription" to it.first
                    )
                }
            })
        }
    }

    /** @restful_api_param_doc
     * @bean_name PlaylistForm
     * # Playlist Request
     * | field name  | type    | required | description |
     * | ----------- | ------- | -------- | ----------- |
     * | isPrivate   | Boolean | false    | default is `false` |
     * | name        | String  | true     | not blank and lesser than 255 chars |
     * | description | String  | false    | not blank and lesser than 255 if set |
     * | coverArtId  | Int     | false    | uploaded cover art id |
     */
    class PlaylistForm(params: Parameters) {
        init {
            validator.validate(params)
        }

        val isPrivate = params["isPrivate"]?.takeIf { it.isBoolean() }?.toBoolean() ?: false
        val name = params["name"]!!
        val description = params["description"]
        val coverArtId = params["coverArtId"]?.toLongOrNull()

        companion object {
            val validator = Validator {
                optional("isPrivate") with isString { it.isBoolean() }
                "name" with isString { it.isNotBlank() && it.length <= 255 }
                optional("description") with isString { it.isNotBlank() && it.length <= 255 }
                optional("coverArtId") with isString { it.isNumber() }
            }
        }
    }

    @KtorRoute
    fun Route.createPlaylist() = accessControl(LibraryScope.PlaylistCreate) {
        /** @restful_api_doc
         * # Create Playlist
         * [POST] /api/playlist
         * ## Body
         * @bean(PlaylistForm)
         * ## Returns
         * Created @bean(PlaylistData.Bean)
         * ```
         * { playlist : @bean(PlaylistData.Bean) }
         * ```
         */
        post {
            val form = PlaylistForm(call.receiveParameters())
            val session = call.acUserIdentityNotNull
            val result = background {
                playlistService.create(session.userId, form.name, form.description, form.coverArtId, form.isPrivate)
            }
            call.respond(mapOf("playlist" to result))
        }
    }

    /** @restful_api_doc
     * # Get Playlist
     * [POST] /api/playlist/{id}
     * ## Parameters
     * - id : playlist id
     * ## Returns
     * @bean(PlaylistData.Bean) with @bean(UserProfile)
     * ```
     * { playlist: @bean(PlaylistData.Bean), creator: @bean(UserProfile) }
     * ```
     */
    @KtorRoute("/{id}")
    fun Route.getPlaylist() = get {
        val id = call.parameters["id"]!!.toLong()
        val result = background { playlistService.findById(id) } ?: validationError("invalid_id")

        if (result.isPrivate) {
            val userIdentity = call.acUserIdentity ?: CR.Error.forbidden()
            if (result.creatorId != userIdentity.userId) CR.Error.forbidden("playlist_is_private")
            val appToken = call.appToken
            if (appToken != null && !appToken.scope.contains(LibraryScope.PlaylistRead.scope)) CR.Error.forbidden()
        }

        val creator = userService.findProfileBeanOf(result.creatorId)
        call.respond(mapOf("playlist" to result, "creator" to creator))
    }

    @KtorRoute("/{id}")
    fun Route.updatePlaylist() = accessControl(LibraryScope.PlaylistUpdate) {
        /** @restful_api_doc
         * # Update playlist base info
         * [POST] /api/playlist/{id}
         * ## Parameters
         * - id : playlist id
         * ## Body
         * @bean(PlaylistForm)
         * ## Returns
         * New state of @bean(PlaylistData.Bean)
         * ```
         * { playlist: @bean(PlaylistData.Bean) }
         * ```
         */
        post {
            val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
            val form = PlaylistForm(call.receiveParameters())
            val session = call.acUserIdentityNotNull
            if (!playlistService.isUserOwnPlaylist(id, session.userId)) CR.Error.forbidden()
            val result = background {
                playlistService.update(id, session.userId, form.name, form.description, form.coverArtId, form.isPrivate)
            }
            call.respond(mapOf("playlist" to result))
        }
    }

    @KtorRoute("/{id}")
    fun Route.deletePlaylist() = accessControl(LibraryScope.PlaylistDelete) {
        /** @restful_api_doc
         * # Delete a playlist
         * [DELETE] /api/playlist/{id}
         * ## Parameters
         * - id : playlist id
         * ## Body
         * @bean(PlaylistForm)
         * ## Returns
         * New state of @bean(PlaylistData.Bean)
         * ```
         * { playlist: @bean(PlaylistData.Bean) }
         * ```
         */
        delete {
            val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
            val session = call.acUserIdentityNotNull
            val result = background {
                playlistService.delete(id, session.userId)
            }
            call.respond(CR.successOrFailed(result))
        }
    }

    @KtorRoute("/{id}/item")
    fun Route.listPlaylistItem() = accessControl(LibraryScope.PlaylistRead) {
        /** @restful_api_doc
         * # Get items in playlist
         * [GET] /api/playlist/{id}/item
         * ## Parameters
         * - id : playlist id
         * ## Sort params
         * - order: playlist item order
         * - id : playlist item id
         * ## Returns
         * @bean(Page) of @bean(PlaylistItemData.Bean)
         * ```
         * { ..., content: [{ playlistItem: @bean(PlaylistItemData.Bean) }] }
         * ```
         */
        get {
            val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(PlaylistItemData.sortOptions)
            val session = call.acUserIdentity
            val result = background {
                val playList = playlistService.findById(id) ?: validationError("invalid_id")
                if (playList.isPrivate && (session == null || playList.creatorId != session.userId)) CR.Error.forbidden()
                playlistService.findAllPlaylistItem(id, paging, sorting).convert {
                    mapOf("playlistItem" to it)
                }
            }

            call.respond(result)
        }
    }

    /** @restful_api_param_doc
     * @bean_name PlayListItemForm
     * # PlayListItem create
     * | field name  | type       | required | description |
     * | ----------- | ------- | -------- | ----------- |
     * | trackId     | Array[Int] | true     | target track id  |
     */
    class PlayListItemForm(params: Parameters) {
        val trackIds = params.getAll("trackId")?.mapNotNull { it.toLongOrNull() } ?: validationError("invalid_track_ids")
    }

    @KtorRoute("/{id}/item")
    fun Route.addPlayListItem() = accessControl(LibraryScope.PlaylistUpdate) {
        /** @restful_api_doc
         * # Add item to playlist
         * [POST] /api/playlist/{id}/item
         * ## Parameters
         * - id : playlist id
         * ## Body
         * @bean(PlayListItemForm)
         * ## Returns
         * Success or failed
         * ```json
         * { message: "success" | "failed" }
         * ```
         */
        post {
            val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
            val form = PlayListItemForm(call.receiveParameters())
            val session = call.acUserIdentityNotNull

            if (!playlistService.isUserOwnPlaylist(session.userId, id)) CR.Error.forbidden()
            val result = if (form.trackIds.isEmpty()) false else {
                background {
                    playlistService.addItemsToPlayList(id, form.trackIds)
                }
            }

            call.respond(CR.successOrFailed(result))
        }
    }

    /** @restful_api_param_doc
     * @bean_name PlayListItemDeleteForm
     * # PlaylistItem delete
     * | field name  | type       | required | description |
     * | ----------- | -------    | -------- | ----------- |
     * | itemId      | Array[Int] | true     | playlist item id |
     */
    class PlayListItemDeleteForm(params: Parameters) {
        val itemIds = params.getAll("itemId")?.mapNotNull { it.toLongOrNull() } ?: validationError("invalid_id")
    }

    @KtorRoute("/{id}/item")
    fun Route.deletePlayListItem() = accessControl(LibraryScope.PlaylistDelete) {
        /** @restful_api_doc
         * # Delete playlist item
         * [DELETE] /api/playlist/{id}/item
         * ## Parameters
         * - id : playlist id
         * ## Body
         * @bean(PlayListItemDeleteForm)
         * ## Returns
         * Success or failed
         * ```json
         * { message: "success" | "failed" }
         * ```
         */
        delete {
            val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
            val form = PlayListItemDeleteForm(call.receiveParameters())
            val session = call.acUserIdentityNotNull

            if (!playlistService.isUserOwnPlaylist(session.userId, id)) CR.Error.forbidden()
            val result = if (form.itemIds.isEmpty()) false else {
                playlistService.deleteItemsFromPlaylist(id, form.itemIds)
            }

            call.respond(CR.successOrFailed(result))
        }
    }

    class PlayListItemMoveParams(parameters: Parameters) {
        val id = parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
        val itemId = parameters["itemId"]?.toLongOrNull() ?: validationError("invalid_item_id")
        val targetItemId = parameters["targetItemId"]?.toLongOrNull() ?: validationError("invalid_target_item_id")
    }

    @KtorRoute("/{id}/item/{itemId}")
    fun Route.movePlaylistItemAbove() = accessControl(LibraryScope.PlaylistUpdate) {
        /** @restful_api_doc
         * # Move playlist item above target item
         * [POST] /api/playlist/{id}/{itemId}?action=move_above&targetItemId={targetItemId}
         * ## Parameters
         * - id : playlist id
         * - itemId : playlist item id
         * - targetItemId : target playlist item id
         * ## Returns
         * Success or failed
         * ```json
         * { message: "success" | "failed" }
         * ```
         */
        param("action", "move_above") {
            post {
                val params = PlayListItemMoveParams(call.parameters)
                val session = call.acUserIdentityNotNull

                if (!playlistService.isUserOwnPlaylist(session.userId, params.id)) CR.Error.forbidden()
                val result = background {
                    playlistService.moveItemAbove(params.id, params.itemId, params.targetItemId)
                }
                call.respond(CR.successOrFailed(result))
            }
        }
    }

    @KtorRoute("/{id}/item/{itemId}")
    fun Route.movePlaylistItemBelow() = accessControl(LibraryScope.PlaylistUpdate) {
        param("action", "move_below") {
            /** @restful_api_doc
             * # Move playlist item below target item
             * [POST] /api/playlist/{id}/{itemId}?action=move_below&targetItemId={targetItemId}
             * ## Parameters
             * - id : playlist id
             * - itemId : playlist item id
             * - targetItemId : target playlist item id
             * ## Returns
             * Success or failed
             * ```json
             * { message: "success" | "failed" }
             * ```
             */
            post {
                val params = PlayListItemMoveParams(call.parameters)
                val session = call.acUserIdentityNotNull

                if (!playlistService.isUserOwnPlaylist(session.userId, params.id)) CR.Error.forbidden()
                val result = background {
                    playlistService.moveItemBelow(params.id, params.itemId, params.targetItemId)
                }

                call.respond(CR.successOrFailed(result))
            }
        }
    }

    @KtorRoute
    fun Route.subscribeOrUnsubscribePlaylist() = route("/{id}") {
        /** @restful_api_doc
         * # Subscribe playlist
         * [POST] /api/playlist/{id}?subscribe
         * ## Parameters
         * - id: playlist id
         * ## Returns
         * Success or not
         */
        param("subscribe") {
            accessControl(LibraryScope.PlaylistSubscribe) {
                post {
                    val playlistId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_playlist_id")
                    val userId = call.acUserIdentityNotNull.userId
                    val playlist = playlistService.findById(playlistId) ?: businessError("playlist_not_found:$playlistId")
                    if(playlist.isPrivate) businessError("playlist_is_private")
                    if(playlist.creatorId == userId) return@post call.respond(CR.successOrFailed(false))
                    val result = playlistService.subscribePlaylist(userId, playlistId)
                    call.respond(CR.successOrFailed(result))
                }
            }
        }

        /** @restful_api_doc
         * # Unsubscribe playlist
         * [POST] /api/playlist/{id}?unsubscribe
         * ## Parameters
         * - id: playlist id
         * ## Returns
         * Success or not
         */
        param("unsubscribe") {
            accessControl(LibraryScope.PlaylistUnsubscribe) {
                post {
                    val playlistId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_playlist_id")
                    val userId = call.acUserIdentityNotNull.userId
                    call.respond(CR.successOrFailed(playlistService.unsubscribePlaylist(userId, playlistId)))
                }
            }
        }
    }
}
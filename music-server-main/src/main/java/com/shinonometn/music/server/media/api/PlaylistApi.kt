package com.shinonometn.music.server.media.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.exposed.SortOptionMapping
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.utils.isBoolean
import com.shinonometn.koemans.utils.isNumber
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.media.data.PlaylistItemData
import com.shinonometn.music.server.media.service.PlaylistService
import com.shinonometn.music.server.security.commons.*
import com.shinonometn.music.server.security.service.UserService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/playlist")
class PlaylistApi(private val playlistService: PlaylistService, private val userService: UserService) {

    @KtorRoute
    fun Route.listPlaylists() = accessControl(AC.Scope.PlayListRead) {
        get {
            val identity = call.acUserIdentityNotNull
            val userId = identity.userId
            val paging = call.receivePageRequest()
            call.respond(background {
                playlistService.findAllByUserId(userId, paging).convert {
                    mapOf("playlist" to it)
                }
            })
        }
    }

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
    fun Route.createPlaylist() = accessControl(AC.Scope.PlayListCreate) {
        post {
            val form = PlaylistForm(call.receiveParameters())
            val session = call.acUserIdentityNotNull
            val result = background {
                playlistService.create(session.userId, form.name, form.description, form.coverArtId, form.isPrivate)
            }
            call.respond(mapOf("playlist" to result))
        }
    }

    @KtorRoute("/{id}")
    fun Route.getPlaylist() = accessControl(AC.Scope.PlayListRead) {
        get {
            val id = call.parameters["id"]!!.toLong()
            val result = background {
                playlistService.findById(id)
            } ?: validationError("invalid_id")

            if(result.isPrivate) CR.Error.forbidden()

            val creator = userService.findProfileBeanOf(result.creatorId)
            call.respond(mapOf("playlist" to result, "creator" to creator))
        }
    }

    @KtorRoute("/{id}")
    fun Route.updatePlaylist() = accessControl(AC.Scope.PlayListUpdate) {
        post {
            val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
            val form = PlaylistForm(call.receiveParameters())
            val session = call.acUserIdentityNotNull
            if(!playlistService.isUserOwnPlaylist(id, session.userId)) CR.Error.forbidden()
            val result = background {
                playlistService.update(id, session.userId, form.name, form.description, form.coverArtId, form.isPrivate)
            }
            call.respond(mapOf("playlist" to result))
        }
    }

    @KtorRoute("/{id}")
    fun Route.deletePlaylist() = accessControl(AC.Scope.PlayListDelete) {
        delete {
            val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
            val session = call.acUserIdentityNotNull
            val result = background {
                playlistService.delete(id, session.userId)
            }
            call.respond(CR.successOrFailed(result))
        }
    }

    private val playlistSortOptions = SortOptionMapping {
        "order" associateTo PlaylistItemData.Table.colOrder defaultOrder SortOrder.DESC
        "id" associateTo PlaylistItemData.Table.id defaultOrder SortOrder.DESC
    }

    @KtorRoute("/{id}/item")
    fun Route.listPlaylistItem() = accessControl(AC.Scope.PlayListRead) {
        get {
            val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(playlistSortOptions)
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

    class PlayListItemForm(params: Parameters) {
        val trackIds = params.getAll("trackId")?.mapNotNull { it.toLongOrNull() } ?: validationError("invalid_track_ids")
    }

    @KtorRoute("/{id}/item")
    fun Route.addPlayListItem() = accessControl(AC.Scope.PlayListUpdate) {
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

    class PlayListItemDeleteForm(params: Parameters) {
        val itemIds = params.getAll("itemId")?.mapNotNull { it.toLongOrNull() } ?: validationError("invalid_id")
    }

    @KtorRoute("/{id}/item")
    fun Route.deletePlayListItem() = accessControl(AC.Scope.PlayListDelete) {
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
    fun Route.movePlaylistItemAbove() = accessControl(AC.Scope.PlayListUpdate) {
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
    fun Route.movePlaylistItemBelow() = accessControl(AC.Scope.PlayListUpdate) {
        param("action", "move_below") {
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
}
package com.shinonometn.music.server.api

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
import com.shinonometn.music.server.data.PlaylistItemData
import com.shinonometn.music.server.service.PlaylistService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/playlist")
class PlaylistApi(private val playlistService: PlaylistService) {

    @KtorRoute("/")
    fun Route.listPlaylists() = get {
        val paging = call.receivePageRequest()
        call.respond(background {
            playlistService.findAll(paging).convert {
                mapOf("playlist" to it)
            }
        })
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

    @KtorRoute("/")
    fun Route.createPlaylist() = post {
        val form = PlaylistForm(call.receiveParameters())
        val result = background {
            // TODO owner id should be passed in
            playlistService.create(1, form.name, form.description, form.coverArtId, form.isPrivate)
        }
        call.respond(mapOf("playlist" to result))
    }

    @KtorRoute("/{id}")
    fun Route.updatePlaylist() = post {
        val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
        val form = PlaylistForm(call.receiveParameters())
        val result = background {
            // TODO owner id should be passed in
            playlistService.update(id, 1, form.name, form.description, form.coverArtId, form.isPrivate)
        }
        call.respond(mapOf("playlist" to result))
    }

    @KtorRoute("/{id}")
    fun Route.deletePlaylist() = delete {
        val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
        val result = background {
            // TODO owner id should be passed in
            playlistService.delete(id, 1)
        }
        call.respond(CR.successOrFailed(result))
    }

    private val playlistSortOptions = SortOptionMapping {
        "order" associateTo PlaylistItemData.Table.colOrder defaultOrder SortOrder.DESC
        "id" associateTo PlaylistItemData.Table.id defaultOrder SortOrder.DESC
    }
    @KtorRoute("/{id}/item")
    fun Route.listPlaylistItem() = get {
        val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
        val paging = call.receivePageRequest()
        val sorting = call.receiveSortOptions(playlistSortOptions)

        val result = background {
            playlistService.findAllPlaylistItem(id, paging, sorting).convert {
                mapOf("playlistItem" to it)
            }
        }

        call.respond(result)
    }

    class PlayListItemForm(params: Parameters) {
        val trackIds = params.getAll("trackId")?.mapNotNull { it.toLongOrNull() } ?: validationError("invalid_track_ids")
    }

    @KtorRoute("/{id}/item")
    fun Route.addPlayListItem() = post {
        val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
        val form = PlayListItemForm(call.receiveParameters())
        val result = if(form.trackIds.isEmpty()) false else {
            background {
                playlistService.addItemsToPlayList(id, form.trackIds)
            }
        }

        call.respond(CR.successOrFailed(result))
    }

    class PlayListItemDeleteForm(params : Parameters) {
        val itemIds = params.getAll("itemId")?.mapNotNull { it.toLongOrNull() } ?: validationError("invalid_id")
    }

    @KtorRoute("/{id}/item")
    fun Route.deletePlayListItem() = delete {
        val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
        val form = PlayListItemDeleteForm(call.receiveParameters())
        val result = if(form.itemIds.isEmpty()) false else {
            playlistService.deleteItemsFromPlaylist(id, form.itemIds)
        }

        call.respond(CR.successOrFailed(result))
    }

    class PlayListItemMoveParams(parameters: Parameters) {
        val id = parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
        val itemId = parameters["itemId"]?.toLongOrNull() ?: validationError("invalid_item_id")
        val targetItemId = parameters["targetItemId"]?.toLongOrNull() ?: validationError("invalid_target_item_id")
    }

    @KtorRoute("/{id}/item/{itemId}")
    fun Route.movePlaylistItemAbove() = param("action", "move_above") {
        post {
            val params = PlayListItemMoveParams(call.parameters)

            val result = background {
                playlistService.moveItemAbove(params.id, params.itemId, params.targetItemId)
            }
            call.respond(CR.successOrFailed(result))
        }
    }

    @KtorRoute("/{id}/item/{itemId}")
    fun Route.movePlaylistItemBelow() = param("action", "move_below") {
        post {
            val params = PlayListItemMoveParams(call.parameters)

            val result = background {
                playlistService.moveItemBelow(params.id, params.itemId, params.targetItemId)
            }

            call.respond(CR.successOrFailed(result))
        }
    }
}
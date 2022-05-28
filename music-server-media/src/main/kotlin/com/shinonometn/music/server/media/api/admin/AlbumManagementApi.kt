package com.shinonometn.music.server.media.api.admin

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.utils.isNumber
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.koemans.web.vararg
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.media.MediaScope
import com.shinonometn.music.server.media.service.AlbumService
import com.shinonometn.music.server.platform.security.commons.accessControl
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta/album")
class AlbumManagementApi(private val albumService: AlbumService) {
/* Album */

    class AlbumCreateRequest(param: Parameters) {

        init {
            validator.validate(param)
        }

        val title: String = param["title"]!!
        val albumArtIds = param.getAll("albumArtId")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
        val albumArtistIds = param.getAll("albumArtistId")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

        companion object {
            val validator = Validator(Validator.Policy.Vararg) {
                "title" with listOf(
                    isString("title_should_not_be_blank") { it.isNotBlank() },
                    isString("length_should_in_255_char") { it.length <= 255 }
                )

                optional("albumArtId") with vararg { it.isNumber() }

                optional("albumArtistId") with vararg { it.isNumber() }
            }
        }
    }

    @KtorRoute
    fun Route.albumApis() = accessControl(MediaScope.Admin.AlbumManagement) {
        post {
            val request = AlbumCreateRequest(call.receiveParameters())
            val result = background { albumService.createAlbum(request.title, request.albumArtIds, request.albumArtistIds) }
            call.respond(mapOf("album" to result))
        }

        route("/{id}") {
            post {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val request = AlbumCreateRequest(call.receiveParameters())
                val result = background { albumService.updateAlbum(id, request.title, request.albumArtIds, request.albumArtistIds) }
                call.respond(mapOf("album" to result))
            }

            delete {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val result = background { albumService.deleteAlbumById(id) } > 0
                call.respond(CR.successOrFailed(result))
            }
        }
    }
}
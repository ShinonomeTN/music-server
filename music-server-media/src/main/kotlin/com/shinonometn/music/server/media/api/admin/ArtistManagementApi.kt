package com.shinonometn.music.server.media.api.admin

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.utils.isNumber
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.commons.vararg
import com.shinonometn.music.server.media.service.MetaManagementService
import com.shinonometn.music.server.platform.security.commons.AC
import com.shinonometn.music.server.platform.security.commons.accessControl
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta/artist")
class ArtistManagementApi(private val service: MetaManagementService) {
    /* Artist */

    class CreateArtistRequest(params: Parameters) {
        init {
            validator.validate(params)
        }

        val name = params["name"]!!
        val coverArtIds = params.getAll("coverArtIds")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

        companion object {
            val validator = Validator(Validator.Policy.Vararg) {
                "name" with listOf(
                    isString("name_should_in_255_char") { it.length <= 255 },
                    isString("name_should_not_be_blank") { it.isNotBlank() },
                )

                optional("coverArtId") with vararg { it.isNumber() }
            }
        }
    }

    @KtorRoute
    fun Route.artistApis() = accessControl(AC.Scope.Admin.ArtistManagement) {
        post {
            val request = CreateArtistRequest(call.receiveParameters())
            val artist = background { service.createArtist(request.name, request.coverArtIds) }
            call.respond(mapOf("artist" to artist))
        }

        route("/{id}") {
            post {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val request = CreateArtistRequest(call.receiveParameters())
                val artist = background { service.updateArtist(id, request.name, request.coverArtIds) }
                call.respond(mapOf("artist" to artist))
            }

            delete {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val result = background { service.deleteArtist(id) } > 0
                call.respond(CR.successOrFailed(result))
            }
        }
    }
}
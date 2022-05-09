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
@KtorRoute("/api/meta/album")
class PublicArtistApi(private val service: MetaManagementService) {

    @KtorRoute
    fun Route.artistApi() = accessControl(AC.Guest) {
        get {
            val paging = call.receivePageRequest()
            val result = background { service.findAllArtists(paging).convert { mapOf("artist" to it) } }
            call.respond(result)
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val result = background { service.getArtistById(id) }
                call.respond(mapOf("artist" to result))
            }
        }
    }

}
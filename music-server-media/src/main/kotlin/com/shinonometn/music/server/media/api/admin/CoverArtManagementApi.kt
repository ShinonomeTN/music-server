package com.shinonometn.music.server.media.api.admin

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.media.MediaScope
import com.shinonometn.music.server.media.data.CoverArtData
import com.shinonometn.music.server.media.service.CoverArtService
import com.shinonometn.music.server.platform.security.commons.AC
import com.shinonometn.music.server.platform.security.commons.accessControl
import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/cover-art")
class CoverArtManagementApi(private val coverArtService: CoverArtService) {
    private val pictureExtensionNames = Regex(""".+?\.(jpg|JPG|jpeg|JPEG|png|PNG)$""")

    @KtorRoute
    fun Route.coverArtManagementApi() = accessControl(MediaScope.Admin.CoverManagement) {
        get {
            val paging = call.receivePageRequest()
            val result = coverArtService.findAll(paging).convert {
                mapOf("coverArt" to it)
            }

            call.respond(result)
        }

        route("/{id}") {
            delete {
                val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_cover_art_id")
                val result = background {
                    coverArtService.deleteById(id) > 0
                }

                call.respond(CR.successOrFailed(result))
            }
        }

        post {
            val multipart = call.receiveMultipart()
            val uploadedFiles = mutableListOf<CoverArtData.Bean>()
            multipart.forEachPart { partData ->
                if (partData !is PartData.FileItem) return@forEachPart
                val filename = partData.originalFileName ?: return@forEachPart
                if (!pictureExtensionNames.matches(filename)) return@forEachPart
                val stream = partData.streamProvider().buffered()
                val file = background {
                    stream.use {
                        coverArtService.store(it, filename)
                    }
                }
                uploadedFiles.add(file)
            }

            call.respond(uploadedFiles.map { mapOf("coverArt" to it) })
        }
    }
}
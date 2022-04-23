package com.shinonometn.music.server.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.data.CoverArtData
import com.shinonometn.music.server.service.CoverArtService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Controller
@KtorRoute("/api/cover-art")
class CoverArtApi(private val coverArtService: CoverArtService) {

    private val pictureExtensionNames = Regex(""".+?\.(jpg|JPG|jpeg|JPEG|png|PNG)$""")

    @KtorRoute("/")
    fun Route.getCoverArtList() = get {
        val paging = call.receivePageRequest()
        val result = coverArtService.findAll(paging).convert {
            mapOf("coverArt" to it)
        }

        call.respond(result)
    }

    @KtorRoute("/{id}")
    fun Route.deleteCoverArt() = delete {
        val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_cover_art_id")
        val result = background {
            coverArtService.deleteById(id) > 0
        }

        call.respond(CR.successOrFailed(result))
    }

    @KtorRoute("/")
    fun Route.uploadCoverArt() = post {
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

    class CoverArtRequestParams(parameters: Parameters) {
        companion object {
            private val allowedTypes = setOf("jpg", "png")
            private val allowedModes = setOf("fill", "cover", "fit_width", "fit_height")
        }

        val width = parameters["width"]?.toIntOrNull()
        val height = parameters["height"]?.toIntOrNull()
        val backgroundColor = parameters["background"]?.takeIf { it.matches(Regex("^#[A-Za-z0-9]{6}$")) }
        val cropMode = parameters["mode"]?.takeIf { allowedModes.contains(it) }
        val type = parameters["type"]?.takeIf { allowedTypes.contains(it) }?.toLowerCase()

        fun isEmpty() = width == null && height == null && backgroundColor == null && cropMode == null && type == null
    }

    private val pathSegmentBlackList = setOf(".", "..")

    @KtorRoute("/{path...}")
    fun Route.getCoverArt() = get {
        val path = call.parameters.getAll("path")?.takeIf { it.isNotEmpty() } ?: return@get call.respond(HttpStatusCode.BadRequest)
        val realPath = path.filter { it.isNotBlank() && !pathSegmentBlackList.contains(it) }.takeIf { it.isNotEmpty() }?.joinToString("/") { it }
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val parameters = CoverArtRequestParams(call.request.queryParameters)

        if (parameters.isEmpty()) {
            val file = coverArtService.get(realPath) ?: return@get call.respond(HttpStatusCode.NotFound)
            call.respondFile(file)
        } else {
            val contentType = when (parameters.type ?: "jpg") {
                "jpg" -> ContentType.Image.JPEG
                "png" -> ContentType.Image.PNG
                else -> validationError("invalid_picture_type")
            }

            val art = background {
                coverArtService.get(realPath) {
                    val width = parameters.width ?: width
                    val height = parameters.height ?: height

                    parameters.backgroundColor?.let { backgroundColor = Color.decode(it) }

                    parameters.cropMode?.let {
                        when (it) {
                            "fill" -> crop(width, height)
                            "cover" -> {
                                scaleKeepRatio(width, height)
                                alignCenter()
                            }
                            "fit_width" -> scaleFitToWidth(width)
                            "fit_height" -> scaleFitToHeight(height)
                        }
                    }
                }
            } ?: return@get call.respond(HttpStatusCode.NotFound)

            val output = ByteArrayOutputStream()
            background { ImageIO.write(art, parameters.type ?: "jpg", output) }
            call.respondBytes(contentType) { output.toByteArray() }
        }
    }
}
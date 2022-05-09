package com.shinonometn.music.server.media.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.media.service.CoverArtService
import com.shinonometn.music.server.platform.security.commons.AC
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Controller
import java.awt.Color
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Controller
@KtorRoute("/api/cover-art")
class PublicCoverArtApi(private val coverArtService: CoverArtService) {
    class CoverArtRequestParams(parameters: Parameters) {
        companion object {
            private val allowedTypes = setOf("jpg", "png")
            private val allowedModes = setOf("fill", "cover", "fit_width", "fit_height")
        }

        val width = parameters["width"]?.toIntOrNull()
        val height = parameters["height"]?.toIntOrNull()
        val backgroundColor = parameters["background"]?.takeIf { it.matches(Regex("^#[A-Za-z0-9]{6}$")) }
        val cropMode = parameters["mode"]?.takeIf { allowedModes.contains(it) }
        val type = parameters["type"]?.takeIf { allowedTypes.contains(it) }
            ?.toLowerCase()

        fun isEmpty() = width == null && height == null && backgroundColor == null && cropMode == null && type == null
    }

    private val pathSegmentBlackList = setOf(".", "..")

    @KtorRoute("/{path...}")
    fun Route.getCoverArt() = accessControl(AC.Guest) {
        get {
            val pathSuffix = call
                .parameters
                .getAll("path")
                ?.filter { it.isNotBlank() && !pathSegmentBlackList.contains(it) }
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString("/") { it }
                ?: return@get call.respond(HttpStatusCode.BadRequest)

            val parameters = CoverArtRequestParams(call.request.queryParameters)

            if (parameters.isEmpty()) {
                val file = coverArtService.get(pathSuffix) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respondOutputStream(contentType = ContentType.Image.Any) {
                    IOUtils.copy(file, this)
                }
                return@get
            }

            val contentType = when (parameters.type ?: "jpg") {
                "jpg" -> ContentType.Image.JPEG
                "png" -> ContentType.Image.PNG
                else -> validationError("invalid_picture_type")
            }

            val art = background {
                coverArtService.get(pathSuffix) {
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
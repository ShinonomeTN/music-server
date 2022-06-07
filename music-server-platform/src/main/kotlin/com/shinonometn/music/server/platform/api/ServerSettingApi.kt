package com.shinonometn.music.server.platform.api

import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.platform.MusicServerSetting
import com.shinonometn.music.server.platform.settings.PlatformSetting
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
class ServerSettingApi(
    private val setting: PlatformSetting,
    private val musicServerSetting: MusicServerSetting
) {

    /** @restful_api_doc
     * # Get music server preference
     * [GET] /.music_server.json
     * ## Returns
     * @bean(MusicServerSetting) containing preference of this server
     */
    @KtorRoute("/.music_server.json")
    fun Route.serverSettings() = get {
        call.respond(musicServerSetting)
    }

    @KtorRoute("/favicon.ico")
    fun Route.favicon() = get {
        val content = setting.favicon ?: this::class.java.getResourceAsStream("/icons/favicon.ico")?.readBytes() ?: CR.Error.notFound()
        call.respondBytes(content, contentType = ContentType.Image.XIcon)
    }

    @KtorRoute("/config/{path...}")
    fun Route.textResources() {
        get {
            val path = call.parameters.getAll("path")?.joinToString("/") { it } ?: ""
            call.respond(
                FreeMarkerContent(
                    "text_resources.ftl", mapOf(
                        "path" to path,
                    ), contentType = ContentType.Text.Any
                )
            )
        }
    }

    @KtorRoute("/robots.txt")
    fun Route.robotsTxt() = get {
        call.respondText(setting.robotsTxt ?: """
            User-agent: *
            Disallow: /
        """.trimIndent()
        )
    }
}
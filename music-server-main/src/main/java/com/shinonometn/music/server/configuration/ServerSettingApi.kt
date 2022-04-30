package com.shinonometn.music.server.configuration

import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.Jackson
import com.shinonometn.music.server.security.commons.AC
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Controller

@Controller
class ServerSettingApi(private val metaConfig : MetaConfiguration) {

    @KtorRoute("/.music_server.json")
    fun Route.serverSettings() = get {
        call.respond(Jackson {
            "allowGuest" to true
            "host" to metaConfig.resolveHostName()
            "apiScopes" to AC.Scope.scopeDescriptions
        })
    }

    @KtorRoute("/favicon.ico")
    fun Route.favicon() = get {
        call.respondOutputStream(ContentType.Image.XIcon) {
            this::class.java.getResourceAsStream("/icons/favicon.ico").use {
                IOUtils.copy(it, this)
            }
        }
    }

}
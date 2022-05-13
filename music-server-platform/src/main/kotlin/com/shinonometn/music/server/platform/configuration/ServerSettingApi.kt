package com.shinonometn.music.server.platform.configuration

import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.Jackson
import com.shinonometn.music.server.platform.security.commons.scopeDescriptions
import com.shinonometn.music.server.platform.security.service.SecurityService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Controller

@Controller
class ServerSettingApi(
    private val metaConfig : MetaConfiguration,
    securityService: SecurityService
) {
    private val scopeDescriptions = securityService.normalScopes.associate { it.scope to it.descriptions }

    @KtorRoute("/.music_server.json")
    fun Route.serverSettings() = get {
        call.respond(Jackson {
            "allowGuest" to true
            "allowGuestRecordingAccess" to true
            "host" to metaConfig.resolveHostName()
            "apiScopes" to scopeDescriptions
            "apiVersion" to "1.0"
            "protocol" to metaConfig.protocol
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
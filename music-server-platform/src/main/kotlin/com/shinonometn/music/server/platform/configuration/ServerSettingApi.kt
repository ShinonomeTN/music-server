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

    /** @restful_api_doc
     * # Get music server preference
     * [GET] /.music_server.json
     * ## Returns
     * Json containing preference of this server
     * |Field                    | Type           |Description                        |
     * |-----                    |----            |-----------
     * |host                     | String         | Main hostname of this server |
     * |protocol                 | String         | Protocol to communicate to this server, http or https ||
     * |allowGuest               | Boolean        | Is this server allow guest access|
     * |allowGuestRecordingAccess| Boolean        | Is this server allow guest accessing recording resources|
     * |apiScopes                | Array[Object]  | Public Api Scopes |
     * |apiVersion               | String         | Api version |
     * |name                     | String         | Optional, Title of this server |
     * |description              | String         | Optional, Description of this server |
     * |greeting                 | String         | Optional, greeting to user |
     */
    @KtorRoute("/.music_server.json")
    fun Route.serverSettings() = get {
        call.respond(mapOf(
            "host" to metaConfig.resolveHostName(),
            "protocol" to metaConfig.protocol,
            "allowGuest" to metaConfig.allowGuest,
            "allowGuestRecordingAccess" to metaConfig.allowGuestRecordingAccess,
            "apiScopes" to scopeDescriptions,
            "apiVersion" to "1.0",
            "name" to metaConfig.name.takeIf { it.isNotBlank() },
            "description" to metaConfig.description.takeIf { it.isNotBlank() },
            "greeting" to metaConfig.greeting.takeIf { it.isNotBlank() },
        ))
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
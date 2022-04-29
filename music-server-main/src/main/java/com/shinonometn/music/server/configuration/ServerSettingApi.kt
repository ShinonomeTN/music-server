package com.shinonometn.music.server.configuration

import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.Jackson
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
class ServerSettingApi(private val metaConfig : MetaConfiguration) {

    @KtorRoute("/.music_server.json")
    fun Route.serverSettings() = get {
        call.respond(Jackson {
            "allowGuest" to true
            "host" to metaConfig.resolveHostName()
        })
    }

}
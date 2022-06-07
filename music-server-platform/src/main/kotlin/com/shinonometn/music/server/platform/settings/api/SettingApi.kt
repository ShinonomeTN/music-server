package com.shinonometn.music.server.platform.settings.api

import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.platform.security.PlatformScope
import com.shinonometn.music.server.platform.security.commons.accessControl
import com.shinonometn.music.server.platform.settings.SettingService
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/admin/setting")
class SettingApi(private val settingService: SettingService) {

    @KtorRoute
    fun Route.getSettings() = accessControl(checker = PlatformScope.Admin.SettingManagement) {
        get {
            call.respond(settingService.registry.values.associate { it.name to it.get() })
        }

        get("/{name}") {
            val name = call.parameters["name"] ?: validationError("invalid_property_name")
            val property = settingService.registry[name] ?: validationError("invalid_property_name:$name")
            call.respond(mapOf(
                "name" to name,
                "value" to property.get()
            ))
        }
    }

    @KtorRoute
    fun Route.updateSettings() = accessControl(checker = PlatformScope.Admin.SettingManagement) {
        post("/{name}") {
            val name = call.parameters["name"] ?: validationError("invalid_property_name")
            val form = call.receiveParameters()
            val property = settingService.registry[name] ?: validationError("invalid_property_name:$name")
            val value = form["value"]
            property.setString(value)
            call.respond(mapOf(
                "name" to name,
                "value" to property.get()
            ))
        }
    }
}
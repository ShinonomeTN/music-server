package com.shinonometn.music.server.platform.security.configuration

import com.shinonometn.koemans.spring.find
import com.shinonometn.koemans.web.spring.configuration.KtorConfiguration
import com.shinonometn.koemans.web.spring.springContext
import com.shinonometn.ktor.server.access.control.AccessControl
import com.shinonometn.music.server.platform.security.commons.UserIdentity
import com.shinonometn.music.server.platform.security.service.UserService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.util.concurrent.TimeUnit

@Configuration
open class SecurityServiceConfiguration {

    @Value("\${application.security.tokenTimeoutSeconds:3600}")
    var appTokenTimeoutSeconds: Long = TimeUnit.DAYS.toSeconds(1)
        private set

    val appTokenTimeoutTimestamp: Long
        get() = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(appTokenTimeoutSeconds)

    @Value("\${application.security.appTokenSalt:}")
    var appTokenSalt: String = "01189998819991197253"
        private set


    @Value("\${application.security.sessionSalt:}")
    var sessionSalt: String = "buyaoyongroot"
        private set

    @Value("\${application.security.sessionTimeoutDays:}")
    var sessionTimeoutDays: Long = 7
        private set

    val sessionTimeoutMillis: Long
        get() = TimeUnit.DAYS.toMillis(sessionTimeoutDays)

    val sessionTimeoutSeconds: Long
        get() = TimeUnit.DAYS.toSeconds(sessionTimeoutDays)

    val sessionTimeoutTimestamp: Long
        get() = System.currentTimeMillis() + sessionTimeoutMillis

    @KtorConfiguration
    fun Application.accessControl() = install(AccessControl) {
        val config = springContext.find<SecurityServiceConfiguration>()
        val userService = springContext.find<UserService>()
        provider("Guest") {
            it.put(com.shinonometn.music.server.platform.security.commons.GuestToken)
        }

        provider("UserSession") {
            val cookie = call.request.cookies["session"] ?: return@provider
            val session = try {
                com.shinonometn.music.server.platform.security.commons.UserSession.from(cookie, config.sessionSalt)
            } catch (e: Exception) {
                // Ignore
                return@provider
            }
            if (!userService.isSessionValid(session.sessionId)) return@provider
            it.put(session)
        }

        provider("AppToken") {
            val token = call.request.headers["X-APP-TOKEN"] ?: return@provider
            val appToken = try {
                com.shinonometn.music.server.platform.security.commons.AppToken.from(token, config.appTokenSalt)
            } catch (e: Exception) {
                // Ignore
                return@provider
            }
            if (!userService.isAppTokenValid(appToken.tokenId)) return@provider
            it.put(appToken)
        }

        provider("User") { context ->
            val identity = context.meta.filterIsInstance<UserIdentity>().firstOrNull() ?: return@provider
            val user = userService.findUserById(identity.userId)?.takeIf { it.enabled } ?: return@provider
            context.put(user)
        }

        onUnAuthorized {
            if (it.rejectReasons().isNotEmpty()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden") + it.rejectReasons())
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized", "message" to "unauthorized"))
            }
        }
    }
}
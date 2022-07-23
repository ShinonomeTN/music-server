package com.shinonometn.music.server.platform.security.configuration

import com.shinonometn.koemans.spring.find
import com.shinonometn.koemans.web.spring.configuration.KtorConfiguration
import com.shinonometn.koemans.web.spring.springContext
import com.shinonometn.ktor.server.access.control.AccessControl
import com.shinonometn.music.server.commons.nullIfError
import com.shinonometn.music.server.platform.security.PlatformScope
import com.shinonometn.music.server.platform.security.api.OAuthSession
import com.shinonometn.music.server.platform.security.commons.*
import com.shinonometn.music.server.platform.security.service.UserService
import com.shinonometn.music.server.platform.settings.PlatformSetting
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.response.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
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

    @Bean
    open fun platformScopes(): Collection<ACScope> = PlatformScope.values().toList() + PlatformScope.Admin.values()

    @KtorConfiguration
    fun Application.accessControl() = install(AccessControl) {
        val config = springContext.find<SecurityServiceConfiguration>()
        val userService = springContext.find<UserService>()
        val settings = springContext.find<PlatformSetting>()

        addMetaExtractor("Guest") {
            if (settings.allowGuest == true) addMeta(GuestToken)
        }

        addMetaExtractor("UserSession") {
            request.cookies["session"]?.let { session ->
                nullIfError {
                    UserSession.from(session, config.sessionSalt)
                }?.takeIf { userService.isSessionValid(it.sessionId) }
            }?.let(::addMeta)
        }

        addMetaExtractor("AppToken") {
            request.headers["X-APP-TOKEN"]?.let { token ->
                nullIfError {
                    AppToken.from(token, config.appTokenSalt)
                }?.takeIf { userService.isAppTokenValid(it.tokenId) }
            }?.let(::addMeta)
        }

        addMetaExtractor("User") {
            meta.filterIsInstance<UserIdentity>().firstOrNull()?.let { identity ->
                userService.findUserById(identity.userId)?.takeIf { it.enabled }
            }?.let(::addMeta)
        }

        addMetaExtractor("TempSession") {
            request.origin.uri.takeIf { it.startsWith("/api/auth") }?.run {
                addMeta(
                    OAuthSession.from(
                        request.call.parameters[OAuthSession.ParameterKey],
                        config.sessionSalt
                    )
                )
            }
        }

        unauthorized {
            if (it.rejectReasons().isNotEmpty()) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "forbidden") + it.rejectReasons())
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "unauthorized", "message" to "unauthorized"))
            }
        }
    }
}
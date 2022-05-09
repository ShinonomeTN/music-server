package com.shinonometn.music.server.platform.security.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.respondPair
import com.shinonometn.music.server.commons.respondSuccessOrFailed
import com.shinonometn.music.server.platform.security.PlatformScope
import com.shinonometn.music.server.platform.security.commons.*
import com.shinonometn.music.server.platform.security.configuration.SecurityServiceConfiguration
import com.shinonometn.music.server.platform.security.service.UserService
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.routing.*
import io.ktor.util.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/auth")
class LoginApi(
    private val userService: UserService,
    private val config: SecurityServiceConfiguration
) {

    @KtorRoute("/login")
    fun Route.userInfo() = accessControl(PlatformScope.UserInfo) {
        get {
            val identity = call.acUserIdentityNotNull
            val user = background { userService.findById(identity.userId) }
            call.respondPair("user" to user)
        }
    }

    @KtorRoute("/login")
    fun Route.login() = post {
        val request = LoginRequest(call.receiveParameters())

        val user = userService.login(request.username, request.password) ?: error("invalid_username_or_password_or_user_disabled")

        val session = UserSession(
            generateNonce() + generateNonce(),
            user.id,
            config.sessionTimeoutTimestamp
        )
        val signedSession = session.sign(config.sessionSalt)

        userService.registerSession(
            session,
            call.request.origin.remoteHost,
            call.request.userAgent() ?: ""
        )

        call.response.cookies.append("session", signedSession, maxAge = config.sessionTimeoutSeconds, path = "/")
        call.respondPair("session" to session)
    }

    @KtorRoute("/login/refresh")
    fun Route.refreshSession() = accessControl("UserSession", checker = AC.HasSession) {
        post {
            val session = call.acUserSessionNotNull
            val newSession = session.copy(config.sessionTimeoutTimestamp)
            val signedSession = newSession.sign(config.sessionSalt)

            userService.registerSession(
                newSession,
                call.request.origin.remoteHost,
                call.request.userAgent() ?: ""
            )
            call.response.cookies.append("session", signedSession, maxAge = config.sessionTimeoutSeconds, path = "/")
            call.respondPair("session" to newSession)
        }
    }

    @KtorRoute("/logout")
    fun Route.logout() = accessControl("UserSession", checker = AC.HasSession) {
        post {
            val session = call.acUserSessionNotNull
            val result = userService.removeSession(session.sessionId) > 0
            call.response.cookies.append("session", "", maxAge = 0L, path = "/")
            call.respondSuccessOrFailed(result)
        }
    }
}
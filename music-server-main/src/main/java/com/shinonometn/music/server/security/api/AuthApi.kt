package com.shinonometn.music.server.security.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.web.ParamValidationException
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.*
import com.shinonometn.music.server.configuration.MetaConfiguration
import com.shinonometn.music.server.security.commons.*
import com.shinonometn.music.server.security.configuration.SecurityServiceConfiguration
import com.shinonometn.music.server.security.service.UserService
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import org.apache.commons.codec.binary.Base64
import org.springframework.stereotype.Controller
import sun.net.www.protocol.http.HttpURLConnection.userAgent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@Controller
@KtorRoute("/api/auth")
class AuthApi(
    private val userService: UserService,
    private val config: SecurityServiceConfiguration,
    private val metaConfig: MetaConfiguration
) {

    class LoginRequest(parameter: Parameters) {

        init {
            validator.validate(parameter)
        }

        val username = parameter["username"]!!
        val password = parameter["password"]!!

        companion object {
            val validator = Validator {
                allowUnknownParams = true
                val usernamePattern = Regex("^[a-zA-Z0-9_]{4,64}$")
                "username" with isString { it.isNotBlank() && it.length <= 64 && usernamePattern.matches(it) }
                "password" with isString { it.isNotBlank() && it.length <= 64 }
            }
        }
    }

    @KtorRoute("/login")
    fun Route.userInfo() = accessControl(AC.Scope.UserInfo) {
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

    private suspend fun ApplicationCall.respondOAuthError(message: String, parameters: Map<String, Any?>) {
        val modal = mapOf(
            "error" to mapOf(
                "message" to Base64.encodeBase64String(message.toByteArray()),
                "details" to parameters
            ),
        )

        respond(
            FreeMarkerContent(
                "oauth_error.ftl", mapOf(
                    "modal" to modal,
                    "modalJson" to background { Jackson.mapper.writeValueAsString(modal) }
                )
            )
        )
    }

    @KtorRoute("/config/{path...}")
    fun Route.textResources() {
        get {
            val path = call.parameters.getAll("path")?.joinToString("/") { it } ?: ""
            val model = mapOf(
                "scopeDescriptions" to AC.Scope.scopeDescriptions,
                "path" to path
            )
            call.respond(FreeMarkerContent("text_resources.ftl", model, contentType = ContentType.Text.Any))
        }
    }

    @KtorRoute
    fun Route.oauth() {
        val tempSessionTimeoutSeconds = TimeUnit.MINUTES.toSeconds(5)

        // The login page
        // input : user_agent, scope, redirect
        get {
            val session = call.acUserSession

            val requestForm = try {
                OAuthRequestForm.from(call)
            } catch (e: OAuthParameterError) {
                return@get call.respondOAuthError(e.message!!, e.parameters)
            }

            // If the user is already logged in, redirect to the confirm page
            if (session != null) {
                val tempSession = OAuthSession(
                    session.userId,
                    requestForm.redirect,
                    System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tempSessionTimeoutSeconds),
                    userAgent,
                    requestForm.scope
                )

                return@get call.respondRedirect("/api/auth?${OAuthSession.ParameterKey}=${tempSession.sign(config.sessionSalt)}")
            }

            val modal = mapOf(
                "session" to mapOf(
                    "userAgent" to requestForm.userAgent,
                    "scopes" to requestForm.scope,
                    "redirect" to requestForm.redirect
                ),
                "state" to "pre_login",
                "scopeDescriptions" to AC.Scope.scopeDescriptions
            )
            call.respond(
                FreeMarkerContent(
                    "oauth_login.ftl", mapOf(
                        "modal" to modal,
                        "modalJson" to background { Jackson.mapper.writeValueAsString(modal) }
                    )
                )
            )
        }

        // OAuth login action
        post {
            val oauthParams = try {
                OAuthRequestForm.from(call)
            } catch (e: OAuthParameterError) {
                return@post call.respondOAuthError(e.message!!, e.parameters)
            }

            val form = call.receiveParameters()
            val loginForm = try {
                LoginRequest(form)
            } catch (e: ParamValidationException) {
                return@post call.respondOAuthError(
                    "Parameter Validation Error.", mapOf(
                        "to" to "user",
                        "recover" to listOf("retry"),
                        "error" to e.error,
                        "message" to e.message
                    )
                )
            }

            val user = userService.login(loginForm.username, loginForm.password)
                ?: return@post call.respondOAuthError(
                    "Invalid username or password.", mapOf(
                        "to" to "user",
                        "recover" to listOf("retry"),
                        "error" to "invalid_credentials",
                        "message" to "invalid_username_or_password"
                    )
                )

            val session = OAuthSession(
                user.id,
                oauthParams.redirect,
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tempSessionTimeoutSeconds),
                userAgent,
                oauthParams.scope
            )

            call.respondRedirect("/api/auth?${OAuthSession.ParameterKey}=${session.sign(config.sessionSalt)}")
        }

        // Confirming OAuth permissions
        param(OAuthSession.ParameterKey) {
            // input : __ts
            get {
                // Check ts exists
                try {
                    val ts = call.parameters["__ts"]
                    val tempSession = OAuthSession.from(ts, config.sessionSalt)

                    // Check user state
                    val user = userService.findUserById(tempSession.userId) ?: throw OAuthParameterError(
                        "User Not Found",
                        mapOf(
                            "to" to "developer",
                            "recover" to listOf("reject")
                        )
                    )

                    if (!user.enabled) throw OAuthParameterError(
                        "Your Account Has Been Disabled",
                        mapOf(
                            "to" to "user",
                            "recover" to listOf("reject", "re_login")
                        )
                    )

                    val modal = mapOf(
                        "user" to user,
                        "session" to tempSession,
                        "sessionSigned" to ts,
                        "state" to "after_login",
                        "scopeDescriptionJson" to AC.Scope.scopeDescriptions
                    )
                    call.respond(
                        FreeMarkerContent(
                            "oauth_confirm.ftl",
                            mapOf(
                                "modal" to modal,
                                "modalJson" to background { Jackson.mapper.writeValueAsString(modal) }
                            )
                        )
                    )

                } catch (e: OAuthParameterError) {
                    return@get call.respondOAuthError(e.message!!, e.parameters)
                } catch (e: Exception) {
                    return@get call.respondOAuthError(
                        "Error: ${e::class.qualifiedName}, reason: ${e.message}. At [${e.firstCauseLineInfo()}].", mapOf(
                            "to" to "maintainer",
                            "recover" to listOf("reject")
                        )
                    )
                }
            }
        }

        // OAuth allow action
        param("action", "allow") {
            post {
                val ts = call.receiveParameters()["ts"] ?: return@post call.respondOAuthError(
                    "Invalid Temp Session", mapOf(
                        "to" to "maintainer",
                        "recover" to listOf("reject")
                    )
                )

                val tempSession = try {
                    OAuthSession.from(ts, config.sessionSalt)
                } catch (e: OAuthParameterError) {
                    return@post call.respondOAuthError(e.message!!, e.parameters)
                }

                val token = userService.registerApiToken(
                    tempSession.userId,
                    tempSession.userAgent,
                    tempSession.scope,
                    LocalDateTime.now().plusSeconds(config.appTokenTimeoutSeconds)
                )

                val appToken =
                    AppToken(token.id, token.userAgent, token.userId, token.scope, token.expiredAt.toInstant(ZoneOffset.UTC).toEpochMilli())
                val stringToken = appToken.sign(config.appTokenSalt)

                val redirect = tempSession.redirect
                if (redirect != "internal") return@post call.respondRedirect("$redirect?state=success&token=${stringToken}&from=com.shinonometn.music.server")
                call.respondRedirect("/api/auth?state=success&token=${stringToken}&from=com.shinonometn.music.server")
            }
        }

        param("state", "success") {
            get {
                call.respond(
                    FreeMarkerContent(
                        "oauth_success.ftl", mapOf(
                            "modal" to "",
                            "modalJson" to "{}"
                        )
                    )
                )
            }
        }
    }

    @KtorRoute("/token/refresh")
    fun Route.tokenRefresh() = accessControl("AppToken", checker = AC.HasToken) {
        post {
            val token = call.appTokenNotNull
            val userAgent = call.request.userAgent() ?: validationError("user_agent_not_found")
            val newToken = token.copy(expireAt = config.appTokenTimeoutTimestamp)
            val signedToken = newToken.sign(config.appTokenSalt)
            userService.refreshApiToken(newToken.tokenId, userAgent, newToken.scope, newToken.expireAt.asLocalDateTime())
            call.respondPair("token" to signedToken)
        }
    }

}
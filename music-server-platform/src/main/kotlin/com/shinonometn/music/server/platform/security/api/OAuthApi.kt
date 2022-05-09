package com.shinonometn.music.server.platform.security.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.web.ParamValidationException
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.Jackson
import com.shinonometn.music.server.commons.asLocalDateTime
import com.shinonometn.music.server.commons.respondPair
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.platform.security.commons.*
import com.shinonometn.music.server.platform.security.configuration.SecurityServiceConfiguration
import com.shinonometn.music.server.platform.security.service.SecurityService
import com.shinonometn.music.server.platform.security.service.UserService
import freemarker.template.TemplateMethodModelEx
import io.ktor.application.*
import io.ktor.freemarker.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.apache.commons.codec.binary.Base64
import org.springframework.stereotype.Controller
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@Controller
@KtorRoute("/api/auth")
class OAuthApi(
    private val userService: UserService,
    private val config: SecurityServiceConfiguration,
    private val securityService: SecurityService,
    private val json: ObjectMapper
) {
    companion object {
        suspend fun respondOAuthError(call: ApplicationCall, message: String, parameters: Map<String, Any?>) {
            val modal = mapOf(
                "error" to mapOf(
                    "message" to Base64.encodeBase64String(message.toByteArray()),
                    "details" to parameters
                ),
            )

            call.respond(
                FreeMarkerContent(
                    "oauth_error.ftl", mapOf(
                        "modal" to modal,
                        "modalJson" to background { Jackson.mapper.writeValueAsString(modal) }
                    )
                )
            )
        }
    }

    @KtorRoute("/config/{path...}")
    fun Route.textResources() {
        val scopeDescriptions = securityService.allScopes.map { it.scope to it.descriptions }.toMap()

        val additionalFunctions = mapOf(
            "scopeDescriptions" to TemplateMethodModelEx {
                json.writeValueAsString(scopeDescriptions)
            }
        )
        get {
            val path = call.parameters.getAll("path")?.joinToString("/") { it } ?: ""
            call.respond(FreeMarkerContent("text_resources.ftl", mapOf(
                "path" to path,
                "ext" to additionalFunctions
            ), contentType = ContentType.Text.Any))
        }
    }

    private val acceptableScopeNames = securityService.allScopes.map { it.scope }
    @KtorRoute
    fun Route.oauth() {
        val tempSessionTimeoutSeconds = TimeUnit.MINUTES.toSeconds(5)

        // The login page
        // input : user_agent, scope, redirect
        get {
            val session = call.acUserSession

            val requestForm = OAuthRequestForm.from(call)
            val scopeList = requestForm.scope
            if (!acceptableScopeNames.containsAll(scopeList)) OAuthError.invalidScopeList(scopeList)

            // If the user is already logged in, redirect to the confirm page
            if (session != null) {
                val tempSession = OAuthSession(
                    session.userId,
                    requestForm.redirect,
                    System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tempSessionTimeoutSeconds),
                    requestForm.userAgent,
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
                "state" to "pre_login"
            )
            call.respond(
                FreeMarkerContent(
                    "oauth_login.ftl", mapOf(
                        "modal" to modal,
                        "modalJson" to background { json.writeValueAsString(modal) }
                    )
                )
            )
        }

        // OAuth login action
        post {
            val oauthParams = OAuthRequestForm.from(call)

            val form = call.receiveParameters()
            val loginForm = try {
                LoginRequest(form)
            } catch (e: ParamValidationException) {
                OAuthError.parameterValidationError(e)
            }

            val user = userService.login(loginForm.username, loginForm.password) ?: OAuthError.invalidUsernameOrPassword()

            val session = OAuthSession(
                user.id,
                oauthParams.redirect,
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tempSessionTimeoutSeconds),
                oauthParams.userAgent,
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
                    val user = userService.findUserById(tempSession.userId) ?: OAuthError.userNotFound()

                    if (!user.enabled) OAuthError.accountDisabled()

                    val modal = mapOf(
                        "user" to user,
                        "session" to tempSession,
                        "sessionSigned" to ts,
                        "state" to "after_login",
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
                } catch (e: Exception) {
                    OAuthError.exceptionRethrow(e)
                }
            }
        }

        // OAuth allow action
        param("action", "allow") {
            post {
                val ts = call.receiveParameters()["ts"] ?: OAuthError.forMaintainer("Invalid Temp Session")

                val tempSession = OAuthSession.from(ts, config.sessionSalt)

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
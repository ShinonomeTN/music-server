package com.shinonometn.music.server.platform.security.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.web.ParamValidationException
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.AccessControlRequirement
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.ktor.server.access.control.meta
import com.shinonometn.music.server.commons.*
import com.shinonometn.music.server.platform.security.commons.*
import com.shinonometn.music.server.platform.security.configuration.SecurityServiceConfiguration
import com.shinonometn.music.server.platform.security.service.SecurityService
import com.shinonometn.music.server.platform.security.service.UserService
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
            call.respond(background {
                CR.freemarker(
                    "oauth_error.ftl", mapOf(
                        "error" to mapOf(
                            "message" to Base64.encodeBase64String(message.toByteArray()),
                            "details" to parameters
                        ),
                    )
                )
            })
        }
    }

    private val acceptableScopeNames = securityService.allScopes.map { it.scope }

    @KtorRoute
    fun Route.oauth() {
        val tempSessionTimeoutSeconds = TimeUnit.MINUTES.toSeconds(5)

        /** @restful_api_doc
         * # OAuth login page
         * [GET] /api/auth
         * ## Parameters
         * - user_agent: optional, user agent to present
         * - scope : required, oauth scopes
         * - redirect : required, redirect to another page after success. use `internal` for no redirect
         * ## Returns
         * OAuth login page
         */
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

            call.respond(
                CR.freemarker(
                    "oauth_login.ftl", mapOf(
                        "session" to mapOf(
                            "userAgent" to requestForm.userAgent,
                            "scopes" to requestForm.scope,
                            "redirect" to requestForm.redirect
                        ),
                        "state" to "pre_login"
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

            val availableScopes = securityService.normalScopes.filter {
                oauthParams.scope.contains(it.scope)
            }.map { it.scope }.toMutableList()
            availableScopes += securityService.advanceScopes.filter {
                oauthParams.scope.contains(it.scope) && it.grantCondition(user)
            }.map { it.scope }

            val session = OAuthSession(
                user.id,
                oauthParams.redirect,
                System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(tempSessionTimeoutSeconds),
                oauthParams.userAgent,
                availableScopes.toSet()
            )

            call.respondRedirect("/api/auth?${OAuthSession.ParameterKey}=${session.sign(config.sessionSalt)}")
        }

        val tempSessionRequirement = AccessControlRequirement(
            listOf("TempSession"),
            listOf { accept() }
        )
        accessControl(tempSessionRequirement) {
            // Confirming OAuth permissions
            param(OAuthSession.ParameterKey) {
                // input : __ts
                get {
                    // Check ts exists
                    val tempSession = call.accessControl.meta<OAuthSession>() ?: OAuthError.forMaintainer("Unexpected No Temp Session.")

                    // Check user state
                    val user = userService.findUserById(tempSession.userId) ?: OAuthError.userNotFound()

                    if (!user.enabled) OAuthError.accountDisabled()

                    val model = mapOf(
                        "user" to user,
                        "session" to tempSession,
                        "sessionSigned" to tempSession.sign(config.sessionSalt),
                        "state" to "after_login",
                    )
                    call.respond(background { CR.freemarker("oauth_confirm.ftl", model) })
                }
            }
        }

        // OAuth allow action
        param("action", "allow") {
            post {
                val tsString = call.receiveParameters()[OAuthSession.ParameterKey] ?: OAuthError.forMaintainer("Unexpected No Temp Session.")
                val tempSession = OAuthSession.from(tsString, config.sessionSalt)

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
                if (redirect != "internal") return@post call.respondRedirect(
                    "$redirect?state=success&token=${stringToken}&from=com.shinonometn.music.server",
                    permanent = false
                )
                call.respondRedirect("/api/auth?state=success&token=${stringToken}&from=com.shinonometn.music.server", permanent = false)
            }
        }

        param("state", "success") {
            get {
                call.respond(background { CR.freemarker("oauth_success.ftl", emptyMap()) })
            }
        }
    }

    @KtorRoute("/token/refresh")
    fun Route.tokenRefresh() = accessControl(AC.HasAppToken) {
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
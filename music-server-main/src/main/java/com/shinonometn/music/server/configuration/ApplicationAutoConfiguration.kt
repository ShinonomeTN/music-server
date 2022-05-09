package com.shinonometn.music.server.configuration

import com.shinonometn.koemans.web.ParamValidationException
import com.shinonometn.koemans.web.spring.configuration.KtorConfiguration
import com.shinonometn.music.server.commons.BusinessException
import com.shinonometn.music.server.platform.security.api.OAuthApi
import com.shinonometn.music.server.platform.security.api.OAuthError
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@ComponentScan("com.shinonometn.music.server")
@Configuration
open class ApplicationAutoConfiguration {

    @KtorConfiguration
    fun Application.xForwardForSupport() = install(XForwardedHeaderSupport)

    @KtorConfiguration
    fun Application.statusPage() = install(StatusPages) {
        exception<ParamValidationException> {
            call.respond(mapOf("error" to "parameter_validation_error", "message" to it.message))
        }

        exception<OAuthError> {
            OAuthApi.respondOAuthError(call, it.message!!, it.parameters)
        }

        exception<BusinessException> {
            call.respond(mapOf("error" to it.error, "message" to it.message))
        }

        exception<Throwable> {
            application.log.error("Error handling request.", it)
            call.respond(mapOf("error" to it::class.simpleName, "message" to it.message))
        }
    }
}
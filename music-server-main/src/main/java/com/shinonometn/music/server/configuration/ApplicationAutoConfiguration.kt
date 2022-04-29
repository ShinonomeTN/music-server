package com.shinonometn.music.server.configuration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.shinonometn.koemans.web.ParamValidationException
import com.shinonometn.koemans.web.spring.configuration.KtorConfiguration
import com.shinonometn.music.server.commons.BusinessException
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.freemarker.*
import io.ktor.jackson.*
import io.ktor.response.*
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@ComponentScan("com.shinonometn.music.server")
@Configuration
open class ApplicationAutoConfiguration {

    @KtorConfiguration
    fun Application.contentNegotiation() = install(ContentNegotiation) {
        jackson {
            registerModule(KotlinModule())
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    @KtorConfiguration
    fun Application.xForwardForSupport() = install(XForwardedHeaderSupport)

    @KtorConfiguration
    fun Application.statusPage() = install(StatusPages) {
        exception<ParamValidationException> {
            call.respond(mapOf("error" to "parameter_validation_error", "message" to it.message))
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
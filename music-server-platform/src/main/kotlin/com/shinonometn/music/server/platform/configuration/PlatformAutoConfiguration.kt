package com.shinonometn.music.server.platform.configuration

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.shinonometn.koemans.web.spring.configuration.KtorConfiguration
import com.shinonometn.music.server.commons.Jackson
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.jackson.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class PlatformAutoConfiguration {

    @KtorConfiguration
    fun Application.contentNegotiation() = install(ContentNegotiation) {
        jackson {
            registerModule(KotlinModule())
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    @Bean
    open fun objectMapper() = Jackson.mapper

}
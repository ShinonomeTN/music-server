package com.shinonometn.music.server.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.shinonometn.koemans.web.spring.configuration.KtorConfiguration
import com.shinonometn.music.server.commons.Jackson
import freemarker.cache.ClassTemplateLoader
import freemarker.cache.FileTemplateLoader
import freemarker.cache.MruCacheStorage
import freemarker.cache.NullCacheStorage
import freemarker.ext.beans.BeanModel
import freemarker.template.TemplateMethodModelEx
import io.ktor.application.*
import io.ktor.freemarker.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
open class FreemarkerConfiguration(
    @Value("\${application.web.template.dir:}") private val templateDir : String,
    @Value("\${application.web.template.cacheEnabled:true}") private val templateCacheEnabled : Boolean,
    @Value("\${application.web.template.strongCacheSize:4}") private val templateStrongCacheSize : Int,
    @Value("\${application.web.template.softCacheSize:4}") private val templateSoftCacheSize : Int
) {

    @KtorConfiguration
    fun Application.freemarker() = install(FreeMarker) {
        val objectMapper = Jackson.mapper
        val templateDir = templateDir.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.isDirectory }
        templateLoader = if(templateDir != null) FileTemplateLoader(templateDir) else ClassTemplateLoader(this::class.java.classLoader, "template")
        cacheStorage = if(templateCacheEnabled) MruCacheStorage(templateStrongCacheSize, templateSoftCacheSize) else NullCacheStorage()
    }
}
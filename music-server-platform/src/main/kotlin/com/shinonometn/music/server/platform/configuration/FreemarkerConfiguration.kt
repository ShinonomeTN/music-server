package com.shinonometn.music.server.platform.configuration

import com.shinonometn.koemans.web.spring.configuration.KtorConfiguration
import com.shinonometn.music.server.platform.common.FreemarkerExtension
import freemarker.cache.ClassTemplateLoader
import freemarker.cache.FileTemplateLoader
import freemarker.cache.MruCacheStorage
import freemarker.cache.NullCacheStorage
import freemarker.template.Configuration.SQUARE_BRACKET_INTERPOLATION_SYNTAX
import freemarker.template.Configuration.SQUARE_BRACKET_TAG_SYNTAX
import freemarker.template.TemplateModel
import io.ktor.application.*
import io.ktor.freemarker.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.io.File
import kotlin.reflect.full.findAnnotation

@Configuration
open class FreemarkerConfiguration(
    @Value("\${application.web.template.dir:}") private val templateDir: String,
    @Value("\${application.web.template.cacheEnabled:true}") private val templateCacheEnabled: Boolean,
    @Value("\${application.web.template.strongCacheSize:4}") private val templateStrongCacheSize: Int,
    @Value("\${application.web.template.softCacheSize:4}") private val templateSoftCacheSize: Int,
    @Autowired(required = false) extensions : List<TemplateModel>?
) {
    private val freemarkerExtensionRegistry = extensions?.mapNotNull {
            val annotation = it::class.findAnnotation<FreemarkerExtension>() ?: return@mapNotNull null
            annotation.name to it
        }?.toMap() ?: emptyMap()

    @KtorConfiguration
    fun Application.freemarker() = install(FreeMarker) {
        val templateDir = templateDir.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.isDirectory }
        templateLoader = if (templateDir != null) FileTemplateLoader(templateDir) else ClassTemplateLoader(this::class.java.classLoader, "template")
        cacheStorage = if (templateCacheEnabled) MruCacheStorage(templateStrongCacheSize, templateSoftCacheSize) else NullCacheStorage()
        interpolationSyntax = SQUARE_BRACKET_INTERPOLATION_SYNTAX
        tagSyntax = SQUARE_BRACKET_TAG_SYNTAX
        setSharedVariables(mapOf("ext" to freemarkerExtensionRegistry))
    }
}
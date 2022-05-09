package com.shinonometn.music.server.file

import com.shinonometn.music.server.configuration.MetaConfiguration
import com.shinonometn.music.server.platform.file.PlatformFileService
import com.shinonometn.music.server.platform.file.impl.LocalFileService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class FileServiceAutoConfiguration(
    private val config : StorageConfiguration,
    private val metaConfig : MetaConfiguration
) {

    @Bean
    open fun fileService() : PlatformFileService {
        val supportedServices = mapOf<String, (StorageConfiguration) -> PlatformFileService>(
            "local" to this::localFileServiceBuilder
        )

        val type = config.type
        val builder = supportedServices[type] ?: error("Unsupported file storage type '${type}'")
        return builder(config)
    }

    private fun localFileServiceBuilder(config : StorageConfiguration) : PlatformFileService {
        val serverLocation = config
            .serverLocation
            ?.takeIf { it.isNotBlank() }
            ?: "${metaConfig.protocol}://${metaConfig.resolveHostName()}"

        val subPath = config.subPath ?: "/"
        val storageDirectory = config
            .storageDirectory
            ?.takeIf { it.isNotBlank() }
            ?: error("'storageDirectory' is required when using local storage type.")

        return LocalFileService {
            localStorageLocation = storageDirectory
            path = subPath
            host = serverLocation
        }
    }
}
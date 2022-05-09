package com.shinonometn.music.server.file

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class StorageConfiguration {

    @Value("\${application.storage.type:local}")
    var type : String = "local"

    @Value("\${application.storage.serverLocation:}")
    var serverLocation : String? = null

    @Value("\${application.storage.directory:}")
    var storageDirectory : String? = null

    @Value("\${application.storage.subPath:}")
    var subPath : String? = null
}
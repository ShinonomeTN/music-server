package com.shinonometn.music.server.file.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class StorageConfiguration {

    @Value("\${application.storage.type:local}")
    var type : String = "local"

    @Value("\${application.storage.directory:./data}")
    var path : String = "./data"

    @Value("\${application.storage.subPath:/storage}")
    var subPath : String = "/storage"
}
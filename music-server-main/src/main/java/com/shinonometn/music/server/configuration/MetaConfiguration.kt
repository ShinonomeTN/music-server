package com.shinonometn.music.server.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class MetaConfiguration {

    @Value("\${application.hostname:localhost}")
    var hostname : String = "localhost"
}
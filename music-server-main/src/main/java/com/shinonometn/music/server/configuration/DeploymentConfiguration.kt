package com.shinonometn.music.server.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class DeploymentConfiguration {

    @Value("\${ktor.deployment.host:http://localhost:8054}")
    var host : String = "localhost"

    @Value("\${ktor.deployment.port:8054}")
    var port : Int = 8054
}
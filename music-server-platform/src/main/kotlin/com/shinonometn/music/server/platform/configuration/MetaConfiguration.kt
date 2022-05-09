package com.shinonometn.music.server.platform.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class MetaConfiguration(private val deployConfig : DeploymentConfiguration) {

    @Value("\${application.hostname:}")
    var hostname : String = ""

    @Value("\${application.protocol:http}")
    var protocol : String = "http"

    private val commonPorts = listOf(80, 443)
    fun resolveHostName() : String {
        return when {
            hostname.isNotBlank() -> hostname
            deployConfig.port in commonPorts -> deployConfig.host
            else -> "${deployConfig.host}:${deployConfig.port}"
        }
    }
}
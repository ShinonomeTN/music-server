package com.shinonometn.music.server.platform.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class MetaConfiguration(private val deployConfig : DeploymentConfiguration) {

    @Value("\${application.hostname:}")
    var hostname : String = ""
        private set

    @Value("\${application.protocol:http}")
    var protocol : String = "http"
        private set

    @Value("\${application.allowGuest:true}")
    var allowGuest : Boolean = true
        private set

    @Value("\${application.allowGuestRecordingAccess:true}")
    var allowGuestRecordingAccess : Boolean = true
        private set

    @Value("\${application.name:}")
    var name : String = ""
        private set

    @Value("\${application.description:}")
    var description : String = ""
        private set

    @Value("\${application.greeting:}")
    var greeting : String = ""
        private set

    private val commonPorts = listOf(80, 443)
    fun resolveHostName() : String {
        return when {
            hostname.isNotBlank() -> hostname
            deployConfig.port in commonPorts -> deployConfig.host
            else -> "${deployConfig.host}:${deployConfig.port}"
        }
    }
}
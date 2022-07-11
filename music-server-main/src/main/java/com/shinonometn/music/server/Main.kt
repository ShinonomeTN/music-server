package com.shinonometn.music.server

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import com.shinonometn.koemans.spring.propertySourcePlaceholderSupport
import com.shinonometn.koemans.spring.useHoconPropertySource
import com.shinonometn.koemans.web.getEnvironmentHoconConfig
import com.shinonometn.koemans.web.spring.configuration.configureBySpring
import com.shinonometn.music.server.platform.configuration.PlatformAutoConfiguration
import io.ktor.application.*
import org.slf4j.LoggerFactory

fun main(args: Array<String>) {
    (LoggerFactory.getLogger("ROOT") as Logger).level = Level.INFO
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.musicServerMainModule() {
    val hocon = getEnvironmentHoconConfig()
    configureBySpring {
        annotationDriven(PlatformAutoConfiguration::class.java) {
            propertySourcePlaceholderSupport()
            hocon?.let { useHoconPropertySource("ktor", it) }
        }
    }
}
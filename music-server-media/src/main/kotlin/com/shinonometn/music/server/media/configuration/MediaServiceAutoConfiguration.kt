package com.shinonometn.music.server.media.configuration

import com.shinonometn.music.server.media.MediaScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MediaServiceAutoConfiguration {

    @Bean
    open fun mediaScopes() = MediaScope.values().toList() + MediaScope.Admin.values()

}
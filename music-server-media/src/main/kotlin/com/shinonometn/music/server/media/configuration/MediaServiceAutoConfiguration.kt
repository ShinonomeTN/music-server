package com.shinonometn.music.server.media.configuration

import com.shinonometn.music.server.media.MediaScope
import com.shinonometn.music.server.platform.security.commons.ACScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Collections

@Configuration
open class MediaServiceAutoConfiguration {
    @Bean
    open fun mediaScopes() : Collection<ACScope> = MediaScope.values().toList() + MediaScope.Admin.values()
}
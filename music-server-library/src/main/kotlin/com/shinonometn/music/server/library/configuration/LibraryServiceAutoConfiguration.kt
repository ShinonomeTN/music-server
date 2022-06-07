package com.shinonometn.music.server.library.configuration

import com.shinonometn.music.server.library.LibraryScope
import com.shinonometn.music.server.platform.security.commons.ACScope
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class LibraryServiceAutoConfiguration {
    @Bean
    open fun libraryScopes(): Collection<ACScope> = LibraryScope.values().toList()
}
package com.shinonometn.music.server.platform.security.service

import com.shinonometn.music.server.platform.security.commons.ACScope
import com.shinonometn.music.server.platform.security.commons.ACScopeAdvance
import org.springframework.stereotype.Service

@Service
class SecurityService(oathScopes : Collection<Collection<ACScope>>) {
    val allScopes = oathScopes.flatten()

    val advanceScopes = allScopes.filterIsInstance<ACScopeAdvance>()

    val normalScopes = allScopes.filter { it !is ACScopeAdvance }
}
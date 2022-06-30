package com.shinonometn.music.server.search

import com.shinonometn.music.server.platform.security.commons.*

enum class SearchScope(override val scope: String, override val descriptions: Map<String, String>) : ACScope {
    ;

    override val permission: ACChecker = AC@{
        if (!hasIdentity()) return@AC reject()
        if (hasPermission(scope) || hasSession() || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
    }
}
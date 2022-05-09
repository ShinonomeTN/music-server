package com.shinonometn.music.server.platform.security

import com.fasterxml.jackson.databind.node.ObjectNode
import com.shinonometn.music.server.commons.Jackson
import com.shinonometn.music.server.platform.security.commons.*
import com.shinonometn.music.server.platform.security.data.UserData
import com.shinonometn.music.server.platform.security.data.permissionList

enum class PlatformScope(override val scope: String, override val descriptions: ObjectNode) : ACScope {
    UserInfo("user_info", Jackson {
        "title" to "Get User Info"
        "description" to "Get your user information. Including your identity, roles and permissions."
    });

    override val permission: ACChecker = AC@{
        if (!hasIdentity()) return@AC reject()
        if (hasPermission(scope) || hasSession()) accept() else reject("message", "insufficient_permission")
    }

    enum class Admin(override val scope: String, override val descriptions: ObjectNode) : ACScopeAdvance {
        UserManagement("admin_user_management",Jackson {
            "title" to "User Management"
            "description" to "Manage all user on this server."
        });

        override val permission: ACChecker = AC@{
            if (hasPermission(scope) || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
        }

        override val grantCondition: (UserData.Bean) -> Boolean = AC@{
            it.isSuperAdmin() || it.permissionList().contains(scope)
        }
    }
}
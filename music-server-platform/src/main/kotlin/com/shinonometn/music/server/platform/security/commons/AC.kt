package com.shinonometn.music.server.platform.security.commons

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.shinonometn.ktor.server.access.control.AccessControlCheckerContext
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.ktor.server.access.control.meta
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.platform.security.data.UserData
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*

typealias ACChecker = suspend AccessControlCheckerContext.() -> Unit

interface ACScope {
    val scope: String
    val permission: ACChecker
    val descriptions : ObjectNode
}

interface ACScopeAdvance : ACScope {
    val grantCondition : ACChecker
}

fun <T : ACScope> Collection<T>.scopeDescriptions() = map { it.descriptions }

object AC {
    object Constants {
        const val SUPER_ADMIN = "super_admin"
        const val PERMISSION = "permission"
        const val ROLE = "role"
    }

    val HasToken: ACChecker = {
        if (meta<AppToken>() != null) accept() else reject()
    }

    val Guest: ACChecker = {
        if (meta<GuestToken>() != null || hasIdentity()) accept() else reject()
    }

    val HasSession: ACChecker = {
        if (meta<UserSession>() != null) accept() else reject()
    }

    val HasIdentity: ACChecker = {
        if (meta<UserIdentity>() != null) accept() else reject()
    }
}

private fun UserData.Bean.resourceObject(): JsonNode? {
    return resources.takeIf { !it.isEmpty && it.isObject }
}

private fun UserData.Bean.permissionList(): List<String> {
    val permission = resourceObject() ?: return emptyList()
    val list = permission[com.shinonometn.music.server.platform.security.commons.AC.Constants.PERMISSION]?.takeIf { it.isArray } ?: return emptyList()
    return list.map { it.asText() }
}

private fun UserData.Bean.roleList(): List<String> {
    val role = resourceObject() ?: return emptyList()
    val list = role[com.shinonometn.music.server.platform.security.commons.AC.Constants.ROLE]?.takeIf { it.isArray } ?: return emptyList()
    return list.map { it.asText() }
}

fun AccessControlCheckerContext.isSuperAdmin(): Boolean {
    val user = meta<UserData.Bean>() ?: return false
    return user.roleList().contains(com.shinonometn.music.server.platform.security.commons.AC.Constants.SUPER_ADMIN)
}

fun AccessControlCheckerContext.hasPermission(vararg permission: String): Boolean {
    val permissionList = (
            (meta<UserData.Bean>()?.permissionList() ?: emptyList())
                    + (meta<AppToken>()?.scope ?: emptyList())
            ).toSet()

    return permissionList.containsAll(permission.toList())
}

fun AccessControlCheckerContext.hasUserPermission(vararg permission: String): Boolean {
    val user = meta<UserData.Bean>() ?: return false
    val permissions = user.permissionList().takeIf { it.isNotEmpty() } ?: return false
    return permissions.containsAll(permission.toList())
}

fun AccessControlCheckerContext.hasIdentity(): Boolean {
    return meta<UserIdentity>() != null
}

fun AccessControlCheckerContext.hasSession(): Boolean {
    return meta<UserSession>() != null
}

val ApplicationCall.acUserSession: UserSession?
    get() = accessControl.meta<UserSession>()

val ApplicationCall.acUserSessionNotNull: UserSession
    get() = acUserSession ?: CR.Error.noACSession()

val ApplicationCall.appToken: AppToken?
    get() = accessControl.meta<AppToken>()

val ApplicationCall.appTokenNotNull: AppToken
    get() = accessControl.meta<AppToken>() ?: CR.Error.noAppToken()

val ApplicationCall.acUserIdentity: UserIdentity?
    get() = accessControl.meta<UserIdentity>()

val ApplicationCall.acUserIdentityNotNull: UserIdentity
    get() = accessControl.meta<UserIdentity>() ?: CR.Error.noACIdentity()

@ContextDsl
fun Route.accessControl(checker: ACScope, builder: Route.() -> Unit): Route {
    return accessControl(builder = builder, checker = checker.permission, providerNames = emptyArray())
}
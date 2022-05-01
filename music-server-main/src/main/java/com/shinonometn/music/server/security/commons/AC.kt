package com.shinonometn.music.server.security.commons

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.shinonometn.ktor.server.access.control.AccessControlCheckerContext
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.ktor.server.access.control.meta
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.Jackson
import com.shinonometn.music.server.security.data.UserData
import io.ktor.application.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*

private typealias ACC = suspend AccessControlCheckerContext.() -> Unit

interface ACPermission {
    val scopeName: String
    val permission: ACC
}

object AC {
    object Constants {
        const val SUPER_ADMIN = "super_admin"
        const val PERMISSION = "permission"
        const val ROLE = "role"
    }

    enum class Scope(override val scopeName: String, private val allowAnonymous: Boolean, val descriptions: ObjectNode) : ACPermission {
        UserInfo("user_info", false, Jackson {
            "title" to "Get User Info"
            "description" to "Get your user information. Including your identity, roles and permissions."
        }),

        PlayListCreate("create_playlist", false, Jackson {
            "title" to "Create playlists"
            "description" to "Create new playlists."
        }),

        PlayListRead("read_playlist", false, Jackson {
            "title" to "Read playlists"
            "description" to "Read your playlists, including private playlists."
        }),

        PlayListUpdate("update_playlist", false, Jackson {
            "title" to "Update playlists"
            "description" to "Update your playlist."
        }),
        PlayListDelete("delete_playlist", false, Jackson {
            "title" to "Delete playlists"
            "description" to "Delete your playlist."
        });

        companion object {
            val scopeDescriptions = Jackson {
                values().forEach {
                    it.scopeName to it.descriptions
                }
            }

            val allNames = (values().map { it.scopeName } + Admin.values().map { it.scopeName }).toSet()
        }

        enum class Admin(override val scopeName: String) : ACPermission {
            CoverManagement("admin_cover_management"),
            UserManagement("admin_user_management"),
            TrackManagement("admin_track_management"),
            RecordingManagement("admin_recording_management"),
            AlbumManagement("admin_album_management"),
            ArtistManagement("admin_artist_management");

            override val permission: ACC = AC@{
                if (hasPermission(this@Admin.scopeName) || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
            }
        }

        override val permission: ACC = AC@{
            if (allowAnonymous) return@AC accept()

            if (!hasIdentity()) return@AC reject()
            if (hasPermission(this@Scope.scopeName) || hasSession() || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
        }
    }

    val HasToken: ACC = {
        if (meta<AppToken>() != null) accept() else reject()
    }

    val Guest: ACC = {
        if (meta<GuestToken>() != null || hasIdentity()) accept() else reject()
    }

    val HasSession: ACC = {
        if (meta<UserSession>() != null) accept() else reject()
    }

    val HasIdentity: ACC = {
        if (meta<UserIdentity>() != null) accept() else reject()
    }
}

private fun UserData.Bean.resourceObject(): JsonNode? {
    return resources.takeIf { !it.isEmpty && it.isObject }
}

private fun UserData.Bean.permissionList(): List<String> {
    val permission = resourceObject() ?: return emptyList()
    val list = permission[AC.Constants.PERMISSION]?.takeIf { it.isArray } ?: return emptyList()
    return list.map { it.asText() }
}

private fun UserData.Bean.roleList(): List<String> {
    val role = resourceObject() ?: return emptyList()
    val list = role[AC.Constants.ROLE]?.takeIf { it.isArray } ?: return emptyList()
    return list.map { it.asText() }
}

fun AccessControlCheckerContext.isSuperAdmin(): Boolean {
    val user = meta<UserData.Bean>() ?: return false
    return user.roleList().contains(AC.Constants.SUPER_ADMIN)
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
fun Route.accessControl(checker: ACPermission, builder: Route.() -> Unit): Route {
    return accessControl(builder = builder, checker = checker.permission, providerNames = emptyArray())
}
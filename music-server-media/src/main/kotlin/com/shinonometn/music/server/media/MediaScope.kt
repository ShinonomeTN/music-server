package com.shinonometn.music.server.media

import com.shinonometn.music.server.platform.security.commons.*
import com.shinonometn.music.server.platform.security.data.UserData
import com.shinonometn.music.server.platform.security.data.permissionList

enum class MediaScope(override val scope: String, override val descriptions: Map<String, String>) : ACScope {
    ;

    override val permission: ACChecker = AC@{
        if (!hasIdentity()) return@AC reject()
        if (hasPermission(scope) || hasSession() || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
    }

    enum class Admin(override val scope: String, override val descriptions: Map<String, String>) : ACScopeAdvance {
        CoverManagement(
            "admin_cover_management", mapOf(
                "title" to "Cover Art Management",
                "description" to "Upload or delete all cover arts on server."
            )
        ),
        TrackManagement(
            "admin_track_management", mapOf(
                "title" to "Track Management",
                "description" to "Create, update and delete all tracks on this server."
            )
        ),
        RecordingManagement(
            "admin_recording_management", mapOf(
                "title" to "Recoding Management",
                "description" to "Add, update or delete all recordings on this server."
            )
        ),
        AlbumManagement(
            "admin_album_management", mapOf(
                "title" to "Album Management",
                "description" to "Add, update or delete all albums on this server."
            )
        ),
        ArtistManagement(
            "admin_artist_management", mapOf(
                "title" to "Artist Management",
                "description" to "Add, update or delete all artists on this server."
            )
        );

        override val grantCondition: (UserData.Bean) -> Boolean = AC@{
            it.isSuperAdmin() || it.permissionList().contains(scope)
        }

        override val permission: ACChecker = AC@{
            if (hasPermission(scope) || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
        }
    }
}
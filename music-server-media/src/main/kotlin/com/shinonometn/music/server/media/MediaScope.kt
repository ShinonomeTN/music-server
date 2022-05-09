package com.shinonometn.music.server.media

import com.fasterxml.jackson.databind.node.ObjectNode
import com.shinonometn.music.server.commons.Jackson
import com.shinonometn.music.server.platform.security.commons.*

enum class MediaScope(override val scope: String, override val descriptions: ObjectNode) : ACScope {
    PlayListCreate("create_playlist",  Jackson {
        "title" to "Create playlists"
        "description" to "Create new playlists."
    }),

    PlayListRead("read_playlist",  Jackson {
        "title" to "Read playlists"
        "description" to "Read your playlists, including private playlists."
    }),

    PlayListUpdate("update_playlist",  Jackson {
        "title" to "Update playlists"
        "description" to "Update your playlist."
    }),
    PlayListDelete("delete_playlist",  Jackson {
        "title" to "Delete playlists"
        "description" to "Delete your playlist."
    });

    override val permission: ACChecker = AC@{
        if (!hasIdentity()) return@AC reject()
        if (hasPermission(scope) || hasSession() || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
    }

    enum class Admin(override val scope: String, override val descriptions: ObjectNode) : ACScopeAdvance {
        CoverManagement("admin_cover_management", Jackson {
            "title" to "Cover Art Management"
            "description" to "Upload or delete all cover arts on server."
        }),
        UserManagement("admin_user_management",Jackson {
            "title" to "User Management"
            "description" to "Manage all user on this server."
        }),
        TrackManagement("admin_track_management", Jackson {
            "title" to "Track Management"
            "description" to "Create, update and delete all tracks on this server."
        }),
        RecordingManagement("admin_recording_management",Jackson {
            "title" to "Recoding Management"
            "description" to "Add, update or delete all recordings on this server."
        }),
        AlbumManagement("admin_album_management",Jackson {
            "title" to "Album Management"
            "description" to "Add, update or delete all albums on this server."
        }),
        ArtistManagement("admin_artist_management", Jackson {
            "title" to "Artist Management"
            "description" to "Add, update or delete all artists on this server."
        });

        override val grantCondition: ACChecker = AC@{
            if (isSuperAdmin()) return@AC accept()
            if (!hasPermission(scope)) return@AC reject("message", "insufficient_permission")
            accept()
        }

        override val permission: ACChecker = AC@{
            if (hasPermission(scope) || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
        }
    }
}
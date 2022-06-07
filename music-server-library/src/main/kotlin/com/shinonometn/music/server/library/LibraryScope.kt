package com.shinonometn.music.server.library

import com.shinonometn.music.server.platform.security.commons.*

enum class LibraryScope(override val scope: String, override val descriptions: Map<String, String>) : ACScope {
    PlaylistCreate(
        "create_playlist", mapOf(
            "title" to "Create playlists",
            "description" to "Create new playlists."
        )
    ),

    PlaylistRead(
        "read_playlist", mapOf(
            "title" to "Read playlists",
            "description" to "Read your playlists, including private playlists."
        )
    ),

    PlaylistUpdate(
        "update_playlist", mapOf(
            "title" to "Update playlists",
            "description" to "Update your playlist."
        )
    ),

    PlaylistDelete(
        "delete_playlist", mapOf(
            "title" to "Delete playlists",
            "description" to "Delete your playlist."
        )
    ),

    PlaylistSubscribe(
        "subscribe_playlist", mapOf(
            "title" to "Subscribe playlist",
            "description" to "Subscribing playlists."
        )
    ),

    PlaylistUnsubscribe(
        "unsubscribe_playlist", mapOf(
            "title" to "Unsubscribe playlist",
            "description" to "Unsubscribing playlists."
        )
    ),

    Artist(
        "read_artist", mapOf(
            "title" to "List Artists",
            "description" to "Reading your following artists."
        )
    ),

    ArtistFollow(
        "follow_artist", mapOf(
            "title" to "Follow artists",
            "description" to "Subscribing artist updates."
        )
    ),

    ArtistUnfollow(
        "unfollow_artist", mapOf(
            "title" to "Unfollow artists",
            "description" to "Unsubscribing artist updates."
        )
    ),

    Track("read_track", mapOf(
        "title" to "List tracks",
        "description" to "List your favourite tracks"
    )),

    TrackFavourite("favourite_track", mapOf(
        "title" to "Favourite a track",
        "description" to "Add a track to favourite",
    )),

    TrackUnfavoured("unfavoured_track", mapOf(
        "title" to "Remote track from Favourite",
        "description" to "Remote a track from favourite"
    )),

    Album("read_album", mapOf(
        "title" to "List albums",
        "description" to  "List your favourite albums"
    )),

    AlbumFavourite("favourite_album", mapOf(
        "title" to "Favourite a albums",
        "description" to  "List your favourite albums"
    )),

    AlbumUnfavored("unfavored_album", mapOf(
        "title" to "Remote album from Favourite",
        "description" to  "Remote album from Favourite"
    ))
    ;

    override val permission: ACChecker = AC@{
        if (!hasIdentity()) return@AC reject()
        if (hasPermission(scope) || hasSession() || isSuperAdmin()) accept() else reject("message", "insufficient_permission")
    }
}
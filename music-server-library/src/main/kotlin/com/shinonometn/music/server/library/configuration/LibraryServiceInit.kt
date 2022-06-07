package com.shinonometn.music.server.library.configuration

import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.library.data.*
import com.shinonometn.music.server.platform.PlatformAbility
import com.shinonometn.music.server.platform.PlatformInitAction
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.stereotype.Component

@Component
@PlatformAbility(
    "library",
    "User Library",
    "Providing user library data service."
)
class LibraryServiceInit(private val database: SqlDatabase) : PlatformInitAction() {
    override fun init() {
        database {
            SchemaUtils.createMissingTablesAndColumns(
                PlaylistData.Table,
                PlaylistItemData.Table,
                UserSubscribedPlaylistData.Table,
                UserFavoriteTrackData.Table,
                UserFollowedArtistData.Table,
                UserPlayStatisticData.Table
            )
        }
    }
}
package com.shinonometn.music.server.media.configuration

import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.commons.PlatformInitAction
import com.shinonometn.music.server.media.data.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.stereotype.Component

@Component
class MediaServiceInit(private val database: SqlDatabase) : PlatformInitAction() {
    override fun init() {
        database {
            SchemaUtils.createMissingTablesAndColumns(
                AlbumArtCoverData.Table,
                AlbumArtistData.Table,
                AlbumData.Table,
                ArtistCoverArtData.Table,
                ArtistData.Table,
                CoverArtData.Table,
                PlaylistData.Table,
                PlaylistItemData.Table,
                RecordingData.Table,
                TrackArtistData.Table,
                TrackData.Table
            )
        }
    }
}
package com.shinonometn.music.server.media.configuration

import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.media.data.*
import com.shinonometn.music.server.platform.PlatformInitAction
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.stereotype.Component

@Component
class MediaServiceInit(private val database: SqlDatabase) : PlatformInitAction() {
    override fun init() {
        database {
            SchemaUtils.createMissingTablesAndColumns(
                AlbumArtCoverRelation.Table,
                AlbumArtistRelation.Table,
                AlbumData.Table,
                ArtistCoverArtRelation.Table,
                ArtistData.Table,
                CoverArtData.Table,
                PlaylistData.Table,
                PlaylistItemData.Table,
                RecordingData.Table,
                TrackArtistRelation.Table,
                TrackData.Table
            )
        }
    }
}
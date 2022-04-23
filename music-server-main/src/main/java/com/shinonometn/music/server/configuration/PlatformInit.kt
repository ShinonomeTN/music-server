package com.shinonometn.music.server.configuration

import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.commons.PlatformInitAction
import com.shinonometn.music.server.data.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextStartedEvent
import org.springframework.stereotype.Component

@Component
class PlatformInit(
    private val database: SqlDatabase,
    private val eventPublisher: ApplicationEventPublisher
) : ApplicationListener<ContextStartedEvent> {
    private val logger = LoggerFactory.getLogger(PlatformInit::class.java)

    override fun onApplicationEvent(event: ContextStartedEvent) {
        logger.info("Initializing database...")
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

        eventPublisher.publishEvent(PlatformInitAction.InitFinished(this))
    }
}
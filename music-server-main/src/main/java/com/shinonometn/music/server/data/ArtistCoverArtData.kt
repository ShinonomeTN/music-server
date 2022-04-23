package com.shinonometn.music.server.data

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert

object ArtistCoverArtData {
    object Table : LongIdTable("tb_artist_art_cover") {
        val colArtistId = reference("artist_id", ArtistData.Table).index()
        val colCoverId = reference("cover_id", CoverArtData.Table).index()
    }

    fun createRelation(artistId: Long, coverId: Long) {
        Table.insert {
            it[this.colArtistId] = artistId
            it[this.colCoverId] = coverId
        }
    }

    fun removeAllRelationsByArtistId(artistId: Long) {
        Table.deleteWhere {
            Table.colArtistId eq artistId
        }
    }

    fun removeAllRelationsByCoverId(id: Long) {
        Table.deleteWhere {
            Table.colCoverId eq id
        }
    }
}
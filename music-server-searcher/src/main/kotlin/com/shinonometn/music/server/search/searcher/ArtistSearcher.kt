package com.shinonometn.music.server.search.searcher

import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.koemans.exposed.pagingBy
import com.shinonometn.music.server.media.data.ArtistData
import com.shinonometn.music.server.search.common.Searcher
import org.jetbrains.exposed.sql.select
import org.springframework.stereotype.Component

@Component
class ArtistSearcher(private val database : SqlDatabase) : Searcher {
    override val category = "artist.public"
    private val table = ArtistData.Table

    override fun search(keyword: String, paging: PageRequest): Collection<Any> {
        return database {
            table.select { table.colName eq keyword }.pagingBy(paging, countQuery = false) {
                ArtistData.Bean(ArtistData.Entity.wrapRow(it))
            }
        }.content
    }
}
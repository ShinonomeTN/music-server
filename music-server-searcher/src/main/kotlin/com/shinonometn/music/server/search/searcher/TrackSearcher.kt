package com.shinonometn.music.server.search.searcher

import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.koemans.exposed.pagingBy
import com.shinonometn.music.server.media.data.TrackData
import com.shinonometn.music.server.search.common.Searcher
import org.jetbrains.exposed.sql.select
import org.springframework.stereotype.Component

@Component
class TrackSearcher(private val database : SqlDatabase) : Searcher {
    override val category = "track.public"

    private val table = TrackData.Table

    override fun search(keyword: String, paging: PageRequest): Collection<Any> {
        return database {
            table.select { table.colTitle eq keyword }.pagingBy(paging, countQuery = false) {
                TrackData.Bean(TrackData.Entity.wrapRow(it))
            }
        }.content
    }

}
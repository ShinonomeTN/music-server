package com.shinonometn.music.server.search.searcher

import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.koemans.exposed.pagingBy
import com.shinonometn.music.server.library.data.PlaylistData
import com.shinonometn.music.server.search.common.Searcher
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.springframework.stereotype.Component

@Component
class PlaylistSearcher(private val database: SqlDatabase) : Searcher {
    override val category = "playlist.public"

    private val table = PlaylistData.Table

    override fun search(keyword: String, paging: PageRequest): Collection<Any> {
        return table.select {
            (table.colName eq keyword) and (table.colIsPrivate eq false)
        }.pagingBy(paging, countQuery = false) {
            PlaylistData.Bean(PlaylistData.Entity.wrapRow(it))
        }.content
    }
}
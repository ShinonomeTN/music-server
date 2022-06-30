package com.shinonometn.music.server.search.searcher

import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.koemans.exposed.pagingBy
import com.shinonometn.music.server.media.data.AlbumData
import com.shinonometn.music.server.search.common.Searcher
import org.jetbrains.exposed.sql.select
import org.springframework.stereotype.Component

@Component
class AlbumSearcher(private val database: SqlDatabase) : Searcher {
    private val albumTable = AlbumData.Table

    override val category = "album.public"

    override fun search(keyword: String, paging: PageRequest): Collection<Any> {
        return database {
            albumTable.select { albumTable.colName eq keyword }.pagingBy(paging, countQuery = false) {
                AlbumData.Bean(AlbumData.Entity.wrapRow(it))
            }
        }.content
    }
}
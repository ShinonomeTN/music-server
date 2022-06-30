package com.shinonometn.music.server.search.service

import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.music.server.search.common.SearchResponse
import com.shinonometn.music.server.search.common.Searcher
import org.springframework.stereotype.Service

@Service
class SearchService(searchers: Collection<Searcher>) {
    private val searchers: Map<String, Searcher>

    init { this.searchers = searchers.associateBy { it.category } }

    fun search(keyword: String, category: Collection<String> = emptyList(), paging: PageRequest): Collection<SearchResponse> {
        val searchers = if (category.isEmpty()) this.searchers.values else this.searchers.filterKeys { category.contains(it) }.values
        return searchers.map { SearchResponse(it.category, it.search(keyword, paging)) }
    }
}
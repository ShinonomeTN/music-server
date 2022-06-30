package com.shinonometn.music.server.search.common

import com.shinonometn.koemans.exposed.PageRequest

interface Searcher {
    val category : String
    fun search(keyword : String, paging : PageRequest) : Collection<Any>
}
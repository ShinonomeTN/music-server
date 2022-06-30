package com.shinonometn.music.server.search.api

import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.platform.security.commons.AC
import com.shinonometn.music.server.search.service.SearchService
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
class PublicSearchApi(private val searchService: SearchService) {

    @KtorRoute("/api/search")
    fun Route.publicSearch() = accessControl(AC.Guest) {
        get {
            val keyword = call.parameters["keyword"] ?: validationError("keyword_required")
            val paging = call.receivePageRequest()
            val categories = call.parameters.getAll("category") ?: emptyList()

            val result = searchService.search(keyword, categories, paging)

            call.respond(result)
        }
    }
}
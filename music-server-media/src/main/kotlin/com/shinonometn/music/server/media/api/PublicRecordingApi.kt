package com.shinonometn.music.server.media.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.ktor.server.access.control.accessControl
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.media.service.MetaManagementService
import com.shinonometn.music.server.platform.security.commons.AC
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta")
class PublicRecordingApi(private val service: MetaManagementService) {
    /** @restful_api_doc
     * # Get track recordings
     * [GET] /api/meta/track/{id}/recording
     * ## Parameters
     * - id : track id
     * ## Body
     *
     * ## Returns
     * List of @bean(RecordingData.Bean)
     * ```
     * [{ recording : @bean(RecordingData.Bean) }]
     * ```
     */
    @KtorRoute
    fun Route.recordingApi() = accessControl(AC.Guest) {
        route("/track/{id}/recording") {
            get {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val result = background { service.getRecordingsByTrackId(id) }.map {
                    mapOf("recording" to it)
                }
                call.respond(result)
            }
        }
    }
}
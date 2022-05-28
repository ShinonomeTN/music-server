package com.shinonometn.music.server.media.api.admin

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.media.MediaScope
import com.shinonometn.music.server.media.service.RecordingService
import com.shinonometn.music.server.platform.security.commons.accessControl
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/meta")
class RecordingManagementApi(private val recordingService: RecordingService) {

    /* Recording */
    class CreateRecordingRequest(parameters: Parameters) {
        init {
            validator.validate(parameters)
        }

        val protocol = parameters["protocol"]!!
        val server = parameters["server"]!!
        val location = parameters["location"]!!

        companion object {
            val validator = Validator {
                "protocol" with isString { it.isNotBlank() && it.length <= 255 }
                "server" with isString { it.isNotBlank() && it.length <= 255 }
                "location" with isString { it.isNotBlank() }
            }
        }
    }

    @KtorRoute
    fun Route.trackRecordingApis() = accessControl(MediaScope.Admin.TrackManagement) {
        route("/track/{id}/recording") {
            post {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val request = CreateRecordingRequest(call.receiveParameters())
                val result = background { recordingService.addTrackRecording(id, request.protocol, request.server, request.location) }
                call.respond(mapOf("recording" to result))
            }
        }
    }

    @KtorRoute
    fun Route.recordingApis() = accessControl(MediaScope.Admin.RecordingManagement) {
        route("/recording/{id}") {
            post {
                val id = call.parameters["id"]?.toLongOrNull() ?: businessError("id_should_be_number")
                val request = CreateRecordingRequest(call.receiveParameters())
                val result = background { recordingService.updateTrackRecording(id, request.protocol, request.server, request.location) }
                call.respond(mapOf("recording" to result))
            }

            delete {
                val recordingId = call.parameters["recordingId"]?.toLongOrNull() ?: businessError("recordingId_should_be_number")
                val result = background { recordingService.deleteTrackRecording(recordingId) }
                call.respond(CR.successOrFailed(result))
            }
        }
    }
}
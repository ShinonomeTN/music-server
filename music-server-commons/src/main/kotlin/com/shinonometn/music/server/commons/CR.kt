package com.shinonometn.music.server.commons

import io.ktor.application.*
import io.ktor.response.*

object CR {
    fun successOrFailed(boolean: Boolean) = mapOf("message" to if (boolean) "success" else "failed")

    object Error {
        fun noACSession(): Nothing = throw BusinessException("no_ac_session", "unexpected_error")
        fun noACIdentity(): Nothing = throw BusinessException("no_ac_identity", "unexpected_error")
        fun noAppToken(): Nothing = throw BusinessException("no_app_token", "unexpected_error")

        fun unauthenticated(): Nothing = throw BusinessException("unauthenticated", "unauthenticated")
        fun forbidden(): Nothing = throw BusinessException("forbidden", "forbidden")
    }

    internal val self = CR
}

suspend fun ApplicationCall.respondSuccessOrFailed(boolean: Boolean) = respond(CR.successOrFailed(boolean))

suspend fun ApplicationCall.respondPair(pair: Pair<String, Any?>) = respond(mapOf(pair))
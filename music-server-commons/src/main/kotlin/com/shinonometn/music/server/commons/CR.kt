package com.shinonometn.music.server.commons

import io.ktor.application.*
import io.ktor.response.*

object CR {
    fun successOrFailed(boolean: Boolean) = mapOf("message" to if (boolean) "success" else "failed")

    fun successOrFailed(any : Any?) = mapOf("message" to when(any) {
        null -> "failed"
        is Exception -> MicroRpc.newIntent("failed") {
            value("exception")
            value(any.javaClass.name)
            value(any.message ?: "")
        }
        is Number -> if(any.toDouble() > 0) MicroRpc("success") {
            value("count")
            value(any.toString())
        } else MicroRpc("failed") {
            value("count")
            value(any.toString())
        }
        else -> "success"
    })

    object Error {
        fun noACSession(): Nothing = throw BusinessException("no_ac_session", "unexpected_error")
        fun noACIdentity(): Nothing = throw BusinessException("no_ac_identity", "unexpected_error")
        fun noAppToken(): Nothing = throw BusinessException("no_app_token", "unexpected_error")

        fun unauthenticated(): Nothing = throw BusinessException("unauthenticated", "unauthenticated")
        fun forbidden(message : String = "forbidden"): Nothing = throw BusinessException("forbidden", message)
    }

    internal val self = CR
}

suspend fun ApplicationCall.respondSuccessOrFailed(boolean: Boolean) = respond(CR.successOrFailed(boolean))

suspend fun ApplicationCall.respondPair(pair: Pair<String, Any?>) = respond(mapOf(pair))
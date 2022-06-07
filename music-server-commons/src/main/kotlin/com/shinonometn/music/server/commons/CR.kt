package com.shinonometn.music.server.commons

import io.ktor.application.*
import io.ktor.freemarker.*
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

    fun freemarker(template : String, model : Map<String, Any?>) : FreeMarkerContent {
        return FreeMarkerContent(template, mapOf(
            "model" to model,
            "modelJson" to Jackson.mapper.writeValueAsString(model)
        ))
    }

    object Error {
        fun noACSession(): Nothing = throw BusinessException("no_ac_session", "unexpected_error")
        fun noACIdentity(): Nothing = throw BusinessException("no_ac_identity", "unexpected_error")
        fun noAppToken(): Nothing = throw BusinessException("no_app_token", "unexpected_error")

        fun unauthenticated(): Nothing = throw BusinessException("unauthenticated", "unauthenticated")
        fun forbidden(message : String = "forbidden"): Nothing = throw BusinessException("forbidden", message)


        fun notFound(message: String = "not_found") : Nothing = throw BusinessException("resource_not_found", message)

//        fun freemarkerNotFound(message : String, additional : Map<Any, Any?> = emptyMap()) : FreeMarkerContent {
//            val map = mapOf(
//                "error" to "resource_not_found",
//                "message" to message
//            ) + additional
//
//            return FreeMarkerContent("not_found", mapOf(
//                "bean" to map,
//                "beanJson" to Jackson.mapper.writeValueAsString(map)
//            ))
//        }
    }

    internal val self = CR
}

suspend fun ApplicationCall.respondSuccessOrFailed(boolean: Boolean) = respond(CR.successOrFailed(boolean))

suspend fun ApplicationCall.respondPair(pair: Pair<String, Any?>) = respond(mapOf(pair))
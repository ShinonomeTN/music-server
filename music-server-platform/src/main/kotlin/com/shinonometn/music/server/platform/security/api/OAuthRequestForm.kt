package com.shinonometn.music.server.platform.security.api

import io.ktor.application.*
import io.ktor.request.*

class OAuthRequestForm(val userAgent: String, val scope: Set<String>, val redirect : String) {

    companion object {
        fun from(call: ApplicationCall): OAuthRequestForm {
            val userAgent = call.parameters["user_agent"] ?: call.request.userAgent() ?: throw OAuthError(
                "Illegal User-Agent: user_agent is missing.",
                mapOf(
                    "to" to "developer",
                    "recover" to listOf("reject")
                )
            )

            val scopes = call.parameters.getAll("scope")?.flatMap {
                if (it.contains(",")) it.split(",") else listOf(it)
            } ?: emptyList()

            // If the scope list is invalid, return an error
            val scopeList = scopes.takeIf { it.isNotEmpty() }?.toSet()
                ?: throw OAuthError(
                    "Scope list is required.",
                    mapOf(
                        "to" to "developer",
                        "recover" to listOf("reject")
                    )
                )

            val redirect = call.parameters["redirect"] ?: throw OAuthError(
                "Invalid Redirect Address. An url or 'internal' should provided.",
                mapOf(
                    "to" to "developer",
                    "recover" to listOf("reject")
                )
            )

            return OAuthRequestForm(userAgent, scopeList, redirect)
        }
    }
}
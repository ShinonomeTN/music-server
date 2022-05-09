package com.shinonometn.music.server.platform.security.api

import com.shinonometn.koemans.web.ParamValidationException
import com.shinonometn.music.server.commons.firstCauseLineInfo

class OAuthError(message: String, val parameters: Map<String, Any?>) : Exception(message) {
    companion object {

        fun forMaintainer(message : String) : Nothing = throw OAuthError(
            message,
            mapOf(
                "to" to "maintainer",
                "recover" to listOf("reject")
            )
        )

        fun forUser(message : String, error : String, errorMessage : String) : Nothing = throw OAuthError(
            message, mapOf(
                "to" to "user",
                "recover" to listOf(
                    "reject",
                    "retry"
                ),
                "error" to error,
                "message" to errorMessage
            )
        )

        fun accountDisabled() : Nothing = throw OAuthError(
            "Your Account Has Been Disabled",
            mapOf(
                "to" to "user",
                "recover" to listOf("reject", "re_login")
            )
        )

        fun userNotFound() : Nothing = throw OAuthError(
            "User Not Found",
            mapOf(
                "to" to "developer",
                "recover" to listOf("reject")
            )
        )

        fun invalidScopeList(scopeNames : Collection<String>) : Nothing = throw OAuthError(
            "Invalid scope list: ${scopeNames.joinToString(",")}",
            mapOf(
                "to" to "developer",
                "recover" to listOf("reject")
            )
        )

        fun parameterValidationError(e : ParamValidationException) : Nothing = throw OAuthError(
            "Parameter Validation Error.", mapOf(
                "to" to "user",
                "recover" to listOf("retry"),
                "error" to e.error,
                "message" to e.message
            )
        )

        fun invalidUsernameOrPassword() : Nothing = throw OAuthError(
            "Invalid username or password.", mapOf(
                "to" to "user",
                "recover" to listOf("retry"),
                "error" to "invalid_credentials",
                "message" to "invalid_username_or_password"
            )
        )

        fun exceptionRethrow(e : Exception) : Nothing = throw if (e is OAuthError) e else OAuthError(
            "Error: ${e::class.qualifiedName}, reason: ${e.message}. At [${e.firstCauseLineInfo()}].", mapOf(
                "to" to "maintainer",
                "recover" to listOf("reject")
            )
        )
    }
}
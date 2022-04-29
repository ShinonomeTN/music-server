package com.shinonometn.music.server.security.api

import com.shinonometn.koemans.utils.isNumber
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import java.net.URLDecoder
import java.net.URLEncoder

class OAuthSession(val userId: Long, val expireAt: Long, val userAgent: String, val scope: Set<String>) {

    fun sign(secret: String): String {
        val content = "$userId" +
                ":${expireAt}" +
                ":${URLEncoder.encode(userAgent, "UTF-8")}" +
                ":${URLEncoder.encode(scope.joinToString(","), "UTF-8")}"
        val sign = Base64.encodeBase64URLSafeString(DigestUtils.sha256(content + secret))
        val encodedContent = Base64.encodeBase64String(content.toByteArray())
        return "${encodedContent}_${sign}"
    }

    companion object {
        const val ParameterKey = "__ts"

        fun from(string: String?, secret: String): OAuthSession {
            if (string == null) throw OAuthParameterError(
                "Invalid Session Format",
                mapOf(
                    "to" to "maintainer",
                    "recover" to listOf("reject")
                )
            )

            val (content, sign) = string.split("_").takeIf { it.size == 2 }
                ?: throw OAuthParameterError(
                    "Invalid Session Format.", mapOf(
                        "to" to "maintainer",
                        "recover" to listOf("reject")
                    )
                )

            val decodedContent = String(Base64.decodeBase64(content))
            val (userId, expireAt, userAgent, scope) = decodedContent.split(":").takeIf { it.size == 4 }
                ?: throw OAuthParameterError(
                    "Invalid Session Content Format.", mapOf(
                        "to" to "maintainer",
                        "recover" to listOf("reject")
                    )
                )

            if (!(userId.isNumber() && expireAt.isNumber()))
                throw OAuthParameterError(
                    "Invalid Session Content Format.", mapOf(
                        "to" to "maintainer",
                        "recover" to listOf("reject")
                    )
                )

            if (expireAt.toLong() < System.currentTimeMillis())
                throw OAuthParameterError(
                    "Session Expired.", mapOf(
                        "to" to "maintainer",
                        "recover" to listOf("reject")
                    )
                )

            val newSign = Base64.encodeBase64URLSafeString(DigestUtils.sha256("${userId}:${expireAt}:${userAgent}:${scope}" + secret))
            if (newSign != sign) throw OAuthParameterError(
                "Invalid Session Signature.", mapOf(
                    "to" to "maintainer",
                    "recover" to listOf("reject")
                )
            )

            return OAuthSession(
                userId.toLong(),
                expireAt.toLong(),
                URLDecoder.decode(userAgent, "UTF-8"),
                URLDecoder.decode(scope, "UTF-8").split(",").toSet()
            )
        }
    }
}
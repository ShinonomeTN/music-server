package com.shinonometn.music.server.platform.security.api

import com.shinonometn.koemans.utils.isNumber
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import java.net.URLDecoder
import java.net.URLEncoder

class OAuthSession(val userId: Long, val redirect : String, val expireAt: Long, val userAgent: String, val scope: Set<String>) {

    fun sign(secret: String): String {
        val content = "$userId" +
                ":${expireAt}" +
                ":${URLEncoder.encode(userAgent, "UTF-8")}" +
                ":${URLEncoder.encode(scope.joinToString(","), "UTF-8")}" +
                ":${URLEncoder.encode(redirect, "UTF-8")}"
        val sign = Base64.encodeBase64URLSafeString(DigestUtils.sha256(content + secret))
        val encodedContent = Base64.encodeBase64String(content.toByteArray())
        return "${encodedContent}$Separator${sign}"
    }

    companion object {
        const val ParameterKey = "__ts"
        const val Separator = "."

        fun from(string: String?, secret: String): OAuthSession {
            if (string == null) OAuthError.forMaintainer("Invalid Session Format")

            val (content, sign) = string.split(Separator).takeIf { it.size == 2 }
                ?: OAuthError.forMaintainer("Invalid Session Format. Content or Sign Missing.")

            val decodedContent = String(Base64.decodeBase64(content))
            val (userId, expireAt, userAgent, scope, redirect) = decodedContent.split(":").takeIf { it.size == 5 }
                ?: OAuthError.forMaintainer("Invalid Session Content Format.")

            if (!(userId.isNumber() && expireAt.isNumber()))
                OAuthError.forMaintainer("Invalid Session Content Format. Invalid User or Expire Format.")

            if (expireAt.toLong() < System.currentTimeMillis())
                OAuthError.forUser("Session Expired.", "session_expired","current_session_is_expired")

            val newSign = Base64.encodeBase64URLSafeString(DigestUtils.sha256("${userId}:${expireAt}:${userAgent}:${scope}:${redirect}" + secret))
            if (newSign != sign) OAuthError.forMaintainer("Invalid Session Signature.")

            return OAuthSession(
                userId.toLong(),
                URLDecoder.decode(redirect, "UTF-8"),
                expireAt.toLong(),
                URLDecoder.decode(userAgent, "UTF-8"),
                URLDecoder.decode(scope, "UTF-8").split(",").toSet()
            )
        }
    }
}
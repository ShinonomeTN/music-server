package com.shinonometn.music.server.security.commons

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import java.net.URLDecoder
import java.net.URLEncoder

class AppToken(val tokenId: Long, val userAgent: String, override val userId: Long, val scope: Set<String>, val expireAt: Long) : UserIdentity {
    fun sign(secret: String): String {
        val encodedUserAgent = URLEncoder.encode(userAgent, "UTF-8")
        val content = "$tokenId:$encodedUserAgent:$userId:${scope.joinToString(",") { it }}:$expireAt"
        val sign = Base64.encodeBase64URLSafeString(DigestUtils.sha256("$content:$secret".toByteArray()))
        return "${Base64.encodeBase64URLSafeString(content.toByteArray())}.$sign"
    }

    fun copy(userId: Long = this.userId, scope: Set<String> = this.scope, expireAt: Long = this.expireAt): AppToken {
        return AppToken(tokenId, userAgent, userId, scope, expireAt)
    }

    companion object {
        fun from(string: String, secret: String): AppToken {
            val (content, sign) = string.split(".")
                .takeIf { it.size == 2 } ?: throw IllegalArgumentException("Invalid token format")

            val decodedContent = String(Base64.decodeBase64(content))
            val decodedSign = Base64.decodeBase64(sign)
            val newSign = DigestUtils.sha256("$decodedContent:$secret".toByteArray())
            if (!newSign.contentEquals(decodedSign)) throw IllegalArgumentException("Invalid token signature")

            val (tokenId, encodedUserAgent, userId, scope, expireAt) = decodedContent
                .split(":")
                .takeIf { it.size == 5 } ?: throw IllegalArgumentException("Invalid token format")

            val userAgent = URLDecoder.decode(encodedUserAgent, "UTF-8")
            val scopeList = scope.split(",").toSet()

            return AppToken(tokenId.toLong(), userAgent, userId.toLong(), scopeList, expireAt.toLong())
        }
    }
}
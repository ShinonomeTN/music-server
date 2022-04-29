package com.shinonometn.music.server.security.commons

import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils

class UserSession(val sessionId : String, override val userId : Long, val expireAt : Long) : UserIdentity {

    fun sign(key : String) : String {
        val string =  "$sessionId:$userId:$expireAt:$key"
        val hash = Base64.encodeBase64URLSafeString(DigestUtils.sha256(string))
        val session = Base64.encodeBase64URLSafeString("$sessionId:$userId:$expireAt".toByteArray())
        return "$session.$hash"
    }

    fun copy(expireAt : Long) : UserSession {
        return UserSession(sessionId, userId, expireAt)
    }

    companion object {
        fun from(string: String, secret : String) : UserSession {
            val (session, hash) = string.split(".").takeIf { it.size == 2 } ?: error("invalid_session_foramt")
            val (sessionId, userId, expireAt) = String(Base64.decodeBase64(session)).split(":").takeIf { it.size == 3 } ?: error("invalid_session_foramt")
            val computedSign = Base64.encodeBase64URLSafeString(DigestUtils.sha256("$sessionId:$userId:$expireAt:$secret"))
            if (hash != computedSign) throw IllegalArgumentException("invalid_session_sign")
            return UserSession(sessionId, userId.toLong(), expireAt.toLong())
        }
    }
}
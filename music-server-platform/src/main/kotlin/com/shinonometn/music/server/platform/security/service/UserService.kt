package com.shinonometn.music.server.platform.security.service

import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.commons.Jackson
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.platform.PlatformInitAction
import com.shinonometn.music.server.platform.security.commons.UserSession
import com.shinonometn.music.server.platform.security.data.AppTokenData
import com.shinonometn.music.server.platform.security.data.SessionData
import com.shinonometn.music.server.platform.security.data.UserData
import kotlinx.coroutines.*
import org.mindrot.jbcrypt.BCrypt
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class UserService(private val database: SqlDatabase) {

    private val logger = LoggerFactory.getLogger(UserService::class.java)

    private val userServiceJobContext = CoroutineScope(Dispatchers.IO)
    private val userServiceJob = userServiceJobContext.launch(start = CoroutineStart.LAZY) {
        logger.info("User service job started.")
        while (isActive) {
            try {
                var clearCount = 0
                database {
                    clearCount += AppTokenData.clearExpired()
                    clearCount += SessionData.clearExpired()
                }
                if (clearCount > 0) logger.info("Clear $clearCount expired tokens and sessions.")
                delay(5000)
            } catch (e: Exception) {
                logger.info("UserService job meet exception.", e)
            }
        }
    }

    @EventListener(PlatformInitAction.InitFinished::class)
    fun userServiceJobKickStart() {
        if (userServiceJob.isActive) return
        userServiceJob.start()
    }

    fun registerSession(userSession: UserSession, ipAddress: String, userAgent: String) {
        val sessionId = userSession.sessionId
        database {
            if (SessionData.lockSessionId(sessionId)) SessionData.updateSession(sessionId) {
                it[colUserId] = userSession.userId
                it[colIpAddress] = ipAddress
                it[colUserAgent] = userAgent
                it[colExpireAt] = LocalDateTime.ofInstant(Instant.ofEpochMilli(userSession.expireAt), ZoneId.systemDefault())
            } else SessionData.Entity.new {
                this.sessionId = userSession.sessionId
                userId = userSession.userId
                expireAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(userSession.expireAt), ZoneId.systemDefault())
                this.ipAddress = ipAddress
                this.userAgent = userAgent
            }
        }
    }

    fun registerApiToken(userId: Long, userAgent: String, scope: Set<String>, expireAt: LocalDateTime) = database {
        AppTokenData.Bean(AppTokenData.Entity.new {
            this.userId = userId
            this.userAgent = userAgent
            this.scope = scope
            this.expiredAt = expireAt
        })
    }

    fun refreshApiToken(tokenId: Long, userAgent: String, scope: Set<String>, expireAt: LocalDateTime) = database {
        AppTokenData.update(tokenId) {
            it[colUserAgent] = userAgent
            it[colScope] = scope.joinToString(",") { it }
            it[colExpiredAt] = expireAt
        }
    }

    fun findUserById(id: Long): UserData.Bean? {
        return database {
            UserData.findById(id)
        }
    }

    fun login(username: String, password: String): UserData.Bean? {
        val user = database { UserData.Entity.findByUsername(username) } ?: return null
        if (!BCrypt.checkpw(password, user.password)) return null
        if (!user.enabled) return null
        return database { UserData.Bean(user) }
    }

    fun findAll(paging: PageRequest) = database {
        UserData.findAll(paging)
    }

    fun findById(id: Long) = database {
        UserData.findById(id)
    }

    fun update(id: Long, nickname: String?, email: String?, avatar: String?, enabled: Boolean) = database {
        UserData.update(id) {
            it[colNickname] = nickname
            it[colEmail] = email
            it[colAvatar] = avatar
            it[colEnabled] = enabled
        }
    }

    fun createUser(
        username: String,
        password: String,
        nickname: String?,
        email: String?,
        avatar: String?,
        resourcesBuilder: (Jackson.JsonBuilderContext.() -> Unit)? = null
    ): UserData.Bean {
        return database {
            if (UserData.usernameExists(username)) businessError("username_exists")
            UserData.Bean(UserData.Entity.new {
                this.username = username
                this.password = BCrypt.hashpw(password, BCrypt.gensalt())
                this.nickname = nickname
                this.email = email
                this.avatar = avatar

                // Put default settings
                resourcesBuilder?.let {
                    this.resources = Jackson(resourcesBuilder)
                }
            })
        }
    }

    fun removeSession(sessionId: String) = database {
        SessionData.deleteBySessionId(sessionId)
    }

    fun isSessionValid(sessionId: String) = database {
        SessionData.isSessionValid(sessionId)
    }

    fun isAppTokenValid(tokenId: Long) = database {
        AppTokenData.isTokenValid(tokenId)
    }

    fun findProfileBeanOf(creatorId: Long) = database {
        UserData.Entity.findById(creatorId)?.toProfileBean()
    }

}
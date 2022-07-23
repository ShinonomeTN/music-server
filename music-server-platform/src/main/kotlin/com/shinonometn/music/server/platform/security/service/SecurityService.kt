package com.shinonometn.music.server.platform.security.service

import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.commons.ignore
import com.shinonometn.music.server.platform.PlatformInitAction
import com.shinonometn.music.server.platform.security.commons.ACScope
import com.shinonometn.music.server.platform.security.commons.ACScopeAdvance
import com.shinonometn.music.server.platform.security.data.AppTokenData
import com.shinonometn.music.server.platform.security.data.SessionData
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import javax.annotation.PreDestroy

@Service
class SecurityService(
    oathScopes: Collection<Collection<ACScope>>,
    private val database: SqlDatabase
) {
    private val logger = LoggerFactory.getLogger(SecurityService::class.java)

    val allScopes = oathScopes.flatten()

    val advanceScopes = allScopes.filterIsInstance<ACScopeAdvance>()

    val normalScopes = allScopes.filter { it !is ACScopeAdvance }

    private val jobContext = CoroutineScope(Dispatchers.IO)
    private val job = jobContext.launch(start = CoroutineStart.LAZY) {
        logger.info("Security service job started.")
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
                if(e !is CancellationException) logger.info("Security service job meet exception.", e)
            }
        }
    }

    @EventListener(PlatformInitAction.InitFinished::class)
    fun securityServiceJobKickStart() {
        if (job.isActive) return
        job.start()
    }

    @PreDestroy
    fun securityServiceJobStop() {
        if (job.isActive) job.cancel()
        ignore(CancellationException::class) { job.asCompletableFuture().join() }
    }
}
package com.shinonometn.music.server.platform

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextStartedEvent
import org.springframework.stereotype.Component

@Component
class PlatformInit(
    private val eventPublisher: ApplicationEventPublisher,
    private val platformInitActions: Collection<PlatformInitAction>
) : ApplicationListener<ContextStartedEvent> {
    private val logger = LoggerFactory.getLogger(PlatformInit::class.java)

    override fun onApplicationEvent(event: ContextStartedEvent) {
        logger.info("Initializing ...")

        platformInitActions.forEach { it.init() }

        eventPublisher.publishEvent(PlatformInitAction.InitFinished(this))
    }
}
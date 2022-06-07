package com.shinonometn.music.server.platform

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextStartedEvent
import org.springframework.stereotype.Component
import kotlin.reflect.full.findAnnotation

@Component
class Platform(
    private val eventPublisher: ApplicationEventPublisher,
    private val platformInitActions: Collection<PlatformInitAction>
) : ApplicationListener<ContextStartedEvent> {
    private val logger = LoggerFactory.getLogger(Platform::class.java)

    private val abilityRegistry = HashMap<String, PlatformAbility>()

    val abilities : Map<String, PlatformAbility> = abilityRegistry

    override fun onApplicationEvent(event: ContextStartedEvent) {
        logger.info("Initializing ...")
        abilityRegistry.putAll(platformInitActions.mapNotNull { it::class.findAnnotation<PlatformAbility>() }.associateBy { it.identity })
        for(action in platformInitActions) { action.init() }
        eventPublisher.publishEvent(PlatformInitAction.InitFinished(this))
    }
}
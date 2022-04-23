package com.shinonometn.music.server.commons

import org.springframework.context.ApplicationEvent

abstract class PlatformInitAction {
    abstract fun init()

    class InitFinished(source : Any) : ApplicationEvent(source)
}
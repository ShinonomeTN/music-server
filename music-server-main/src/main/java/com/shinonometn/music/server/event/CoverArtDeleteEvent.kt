package com.shinonometn.music.server.event

import org.springframework.context.ApplicationEvent

class CoverArtDeleteEvent(source : Any, val id : Long) : ApplicationEvent(source)
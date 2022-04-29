package com.shinonometn.music.server.media.event

import org.springframework.context.ApplicationEvent

class CoverArtDeleteEvent(source : Any, val id : Long) : ApplicationEvent(source)
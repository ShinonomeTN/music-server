package com.shinonometn.music.server.platform

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class PlatformAbility(
    val identity: String,
    val title: String,
    val description: String
)

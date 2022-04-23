package com.shinonometn.music.server.commons

object CR {
    fun successOrFailed(boolean: Boolean) = mapOf("message" to if (boolean) "success" else "failed")
}
package com.shinonometn.music.server.platform.security.api

class OAuthParameterError(message: String, val parameters: Map<String, Any?>) : Exception(message)
package com.shinonometn.music.server.platform.security.commons

import io.ktor.http.*
import io.ktor.response.*

fun ResponseCookies.appendSession(session: String, maxAge : Long, secure : Boolean) {
    append(
        "session",
        session,CookieEncoding.URI_ENCODING,
        maxAge,
        null,
        null,
        "/",
        secure = secure,
        httpOnly = true,
        extensions = mapOf(
            "Same-Site" to "strict"
        )
    )
}
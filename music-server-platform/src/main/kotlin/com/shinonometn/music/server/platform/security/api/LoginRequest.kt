package com.shinonometn.music.server.platform.security.api

import com.shinonometn.koemans.web.Validator
import io.ktor.http.*

class LoginRequest(parameter: Parameters) {

    init {
        validator.validate(parameter)
    }

    val username = parameter["username"]!!
    val password = parameter["password"]!!

    companion object {
        val validator = Validator {
            allowUnknownParams = true
            val usernamePattern = Regex("^[a-zA-Z0-9_]{4,64}$")
            "username" with isString { it.isNotBlank() && it.length <= 64 && usernamePattern.matches(it) }
            "password" with isString { it.isNotBlank() && it.length <= 64 }
        }
    }
}
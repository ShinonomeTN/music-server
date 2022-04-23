package com.shinonometn.music.server.commons

import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.ValidatorBuilderDsl

@ValidatorBuilderDsl
fun Validator.Configuration.vararg(hint: String = "invalid_value", logic: (String) -> Boolean) = validator(hint) { param ->
    when (param) {
        is String -> logic(param)
        is List<*> -> param.all { logic(it as String) }
        else -> false
    }
}
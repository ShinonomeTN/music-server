package com.shinonometn.music.server.commons

class BusinessException(message: String, val error : String = "business_error") : Exception(message)

fun businessError(message: String) : Nothing = throw BusinessException(message)

fun validationError(message: String) : Nothing = throw BusinessException(message, "validation_error")
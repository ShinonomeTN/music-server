package com.shinonometn.music.server.commons

fun Exception.firstCauseLineInfo() : String {
    val f = stackTrace.firstOrNull() ?: return "No stack trace"
    return "${f.className}.${f.methodName}@${f.fileName}:${f.lineNumber}"
}

fun <T> nullIfError(provider : () -> T) : T? {
    return try {
        provider()
    } catch (e : Exception) {
        null
    }
}
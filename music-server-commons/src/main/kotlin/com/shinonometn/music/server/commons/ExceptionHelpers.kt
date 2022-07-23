package com.shinonometn.music.server.commons

import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

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

fun <E : Throwable, R> ignore(clazz : KClass<E> ,block : () -> R) : R? {
    return try {
        block()
    } catch (e : Exception) {
        if(e::class.isSubclassOf(clazz)) null else throw e
    }
}
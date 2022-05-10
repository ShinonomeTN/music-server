package com.shinonometn.accounting.common

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.*

object MicroRpc {

    abstract class Param<T>(val origin: String) {
        abstract fun read(): T
    }

    object Types {
        class Value(origin: String) : Param<String>(origin) {
            override fun read(): String = URLDecoder.decode(origin, "UTF8")
        }

        class Collection(origin: String) : Param<List<String>>(origin) {
            override fun read(): List<String> = origin.removeSurrounding("[", "]").split(",").map {
                URLDecoder.decode(it, "UTF8")
            }
        }
    }

    class Intent internal constructor(val name: String, val params: List<Param<*>>, private val origin : String) {
        override fun toString() = origin

        companion object {
            internal val EMPTY_INTENT = Intent("", emptyList(), "")
            fun emptyIntent() = EMPTY_INTENT
        }
    }

    private val commandPattern = Regex("^([A-Za-z0-9_.]+?)(:.+|:)?$")

    fun parse(command: String): Intent {
        if(command.isBlank()) return Intent.EMPTY_INTENT

        val parts = commandPattern.matchEntire(command)?.takeIf {
            it.groupValues.size > 1
        }?.groupValues ?: throw MicroRpcTokenizeException("invalid_format", command)

        if (parts.size == 2) return Intent(parts[1], emptyList(), command)

        val name = parts[1]
        val params = parts[2].removePrefix(":")

        val list = LinkedList<Param<*>>()
        val sb = StringBuilder()
        var isList = false
        var index = 0
        while (index < params.length) {
            val c = params[index]
            if (isList) sb.append(c).takeIf { c == ']' }?.let {
                isList = false
                list.add(Types.Collection(it.toString()))
                it.clear()
                index++
            } else when (c) {
                ',' -> list.add(Types.Value(sb.toString())).also { sb.clear() }
                '[' -> {
                    sb.append(c)
                    isList = true
                }
                ']' -> throw MicroRpcTokenizeException("unexpected_quote", command)
                ' ' -> { /* Do nothing just skip */ }
                else -> sb.append(c)
            }
            index++
        }
        if (sb.isNotEmpty()) list.add(
            if (isList) throw MicroRpcTokenizeException("unclosed_quote", command)
            else Types.Value(sb.toString())
        )

        return Intent(name, list, command)
    }

    class CommandBuildContext internal constructor(name: String) {
        private val sb = StringBuilder().append(URLEncoder.encode(name, "UTF8"))

        private var m = false

        fun value(s: String) {
            sb.append(if (m) ',' else ':')
            sb.append(URLEncoder.encode(s, "UTF8"))
            m = true
        }

        fun list(ss: List<String>) {
            sb.append(if (m) ',' else ':')
            sb.append("[${ss.map { URLEncoder.encode(it, "UTF8") }.joinToString(",") { it }}]")
            m = true
        }

        fun list(vararg ss: String) {
            sb.append(if (m) ',' else ':')
            sb.append("[${ss.map { URLEncoder.encode(it, "UTF8") }.joinToString(",") { it }}]")
            m = true
        }

        override fun toString() = sb.toString()
    }

    val IntentNamePattern = Regex("^[A-Za-z0-9_.]+$")

    operator fun invoke(name : String, block: (CommandBuildContext.() -> Unit)? = null) = newIntent(name, block)

    fun newIntent(name: String, block: (CommandBuildContext.() -> Unit)? = null) = CommandBuildContext(name.takeIf {
        it.matches(IntentNamePattern)
    } ?: error("invalid_intent_name")).also {
        block?.invoke(it)
    }.toString()

    class MicroRpcTokenizeException(message: String, val payload: String) : Exception(message)
}
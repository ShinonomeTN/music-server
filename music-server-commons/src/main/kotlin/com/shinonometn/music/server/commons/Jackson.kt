package com.shinonometn.music.server.commons

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

object Jackson {
    val mapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        registerModule(JavaTimeModule())
        disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    val EmptyObject: JsonNode = mapper.createObjectNode()

    operator fun invoke(builder: JsonBuilderContext.() -> Unit): ObjectNode {
        return JsonBuilderContext().apply(builder).build()
    }

    operator fun invoke(vararg items : Any) : ArrayNode {
        val node = mapper.createArrayNode()
        items.forEach {
            val newNode : JsonNode = mapper.valueToTree(it)
            node.add(newNode)
        }
        return node
    }

    class JsonBuilderContext(private val mapper: ObjectMapper = Jackson.mapper) {
        private val node: ObjectNode = mapper.createObjectNode()

        infix fun String.to(value: Any) {
            node.putPOJO(this, value)
        }

        infix fun String.to(value : String) {
            node.put(this, value)
        }

        infix fun String.to(bool : Boolean) {
            node.put(this, bool)
        }

        infix fun String.to(jsonNode: ObjectNode): ObjectNode {
            return node.putObject(this).also {
                it.setAll<JsonNode>(jsonNode)
            }
        }

        infix fun String.to(arrayNode : ArrayNode) {
            node.putArray(this).also {
                it.addAll(arrayNode)
            }
        }

        fun array(vararg items: Any): ArrayNode {
            val node = mapper.createArrayNode()
            items.forEach {
                val newNode : JsonNode = mapper.valueToTree(it)
                node.add(newNode)
            }
            return node
        }

        fun array(vararg strings : String) : ArrayNode {
            val node = mapper.createArrayNode()
            strings.forEach {
                node.add(it)
            }
            return node
        }

        fun array(stringCollection : Collection<String>) : ArrayNode {
            val node = mapper.createArrayNode()
            stringCollection.forEach {
                node.add(it)
            }
            return node
        }

        internal fun build(): ObjectNode {
            return node
        }
    }
}
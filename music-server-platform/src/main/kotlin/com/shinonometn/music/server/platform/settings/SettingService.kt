package com.shinonometn.music.server.platform.settings

import com.fasterxml.jackson.databind.JsonNode
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.platform.settings.data.SettingPropertyData
import org.springframework.stereotype.Service

@Service
class SettingService(private val database : SqlDatabase) {

    val registry = mutableMapOf<String, SettingProperty<*>>()

    @Suppress("UNCHECKED_CAST")
    inline operator fun <reified T> get(name : String) : SettingProperty<T> {
        return try {
            val transformer  = when {
                T::class == String::class -> { SettingProperty.Transformers.String }
                T::class == Int::class -> { SettingProperty.Transformers.Int }
                T::class == Long::class -> { SettingProperty.Transformers.Long }
                T::class == Double::class -> { SettingProperty.Transformers.Double }
                T::class == Boolean::class -> { SettingProperty.Transformers.Boolean }
                T::class == JsonNode::class -> { SettingProperty.Transformers.Json }
                T::class == ByteArray::class -> { SettingProperty.Transformers.Binary }
                else -> error("Unsupported property type: ${T::class.java.name}")
            }
            registry.computeIfAbsent(name) { SettingProperty(name, this, transformer as SettingProperty.Transformer<T>) } as SettingProperty<T>
        } catch (e : ClassCastException) {
            error("Property value type not match")
        }
    }

    fun saveProperty(name : String, value : String?) = database {
        SettingPropertyData[name] = value
    }

    fun getProperty(name : String) : String? = database {
        SettingPropertyData[name]
    }
}
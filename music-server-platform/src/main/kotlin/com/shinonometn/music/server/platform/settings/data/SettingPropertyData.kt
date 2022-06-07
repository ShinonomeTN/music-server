package com.shinonometn.music.server.platform.settings.data

import com.shinonometn.koemans.exposed.countBy
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.update

object SettingPropertyData {
    object Table: IdTable<String>(name = "tb_sys_settings") {
        override val id = varchar("key", 255).entityId()
        val colValue = text("value").nullable()
    }

    fun exists(name: String) : Boolean = Table.countBy(Table.id) { Table.id eq name } >= 0

    operator fun set(name: String, value : String?) {
        val exists = exists(name)
        if(!exists) {
            Table.insert { it[id] = name; it[colValue] = value }
        } else {
            Table.update { it[colValue] = value }
        }
    }

    operator fun get(name: String): String? {
        return Table.select { Table.id eq name }.firstOrNull()?.get(Table.colValue)
    }
}
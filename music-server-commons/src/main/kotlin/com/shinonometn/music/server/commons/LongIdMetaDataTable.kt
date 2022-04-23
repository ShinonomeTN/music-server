package com.shinonometn.music.server.commons

import org.jetbrains.exposed.dao.id.LongIdTable

abstract class LongIdMetaDataTable(name : String) : LongIdTable(name) {
    val colMetaData = text("meta_data").default("{}")
}
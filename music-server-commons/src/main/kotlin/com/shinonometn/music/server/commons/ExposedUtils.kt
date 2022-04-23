package com.shinonometn.music.server.commons

import org.jetbrains.exposed.dao.ColumnWithTransform
import org.jetbrains.exposed.sql.Column

fun Column<String>.transformJsonNode() = ColumnWithTransform(this, { Jackson.mapper.writeValueAsString(it)}, {
    Jackson.mapper.readTree(it)
})

fun Column<String>.transformLongIdList() = ColumnWithTransform(
    this,
    { ids -> ids.joinToString(",") { it.toString() } },
    { string -> string.split(",").map { it.toLong() } }
)
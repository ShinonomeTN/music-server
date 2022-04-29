package com.shinonometn.music.server.commons

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.asLocalDateTime(): LocalDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
package com.shinonometn.music.server.platform.file

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * File representation of platform
 * */
class PlatformFileInfo(
    val externalPath: String,
    val internalPath : String,
    val size: Long,
    private val fileService : PlatformFileService
) {
    fun inputStream() = fileService.inputStreamOf(this)
}
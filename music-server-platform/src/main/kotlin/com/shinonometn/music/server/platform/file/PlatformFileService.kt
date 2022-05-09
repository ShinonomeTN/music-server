package com.shinonometn.music.server.platform.file

import java.io.InputStream

interface PlatformFileService {
    fun store(inputStream: InputStream, originFilename: String): PlatformFileInfo
    fun get(suffix : String) : PlatformFileInfo
    fun delete(suffix: String) : Boolean

    fun inputStreamOf(file : PlatformFileInfo) : InputStream?
}
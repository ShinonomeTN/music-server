package com.shinonometn.music.server.build.document.handler

import java.io.File

interface FileHandler {
    fun isFileSupported(file : File): Boolean
    fun parse(file : File) : Collection<DocFragment>
}
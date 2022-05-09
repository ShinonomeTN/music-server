package com.shinonometn.music.server.commons

import org.apache.commons.io.IOUtils
import java.io.File

fun File.copyTo(another : File) {
    val source = inputStream()
    val target = another.outputStream()
    try {
        IOUtils.copy(source, target)
    } finally {
        source.close()
        target.close()
    }
}
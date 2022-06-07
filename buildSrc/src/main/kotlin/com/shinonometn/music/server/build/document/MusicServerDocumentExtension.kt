package com.shinonometn.music.server.build.document

import com.shinonometn.music.server.build.document.handler.FileHandler
import com.shinonometn.music.server.build.document.handler.JavaDocFileHandler

class MusicServerDocumentExtension {
    class ScannerTask(val handler : FileHandler) {
        internal val directories = mutableListOf<String>()

        fun scan(directory: String) {
            directories.add(directory)
        }
    }

    infix operator fun invoke(configure: MusicServerDocumentExtension.() -> Unit) {
        this.apply(configure)
    }

    internal val scannerTasks = mutableListOf<ScannerTask>()

    fun javaDoc(configure: ScannerTask.() -> Unit) {
        val task = ScannerTask(JavaDocFileHandler)
        task.apply(configure)
        scannerTasks.add(task)
    }

    companion object {
        const val MODEL_NAME = "musicServerDocument"
    }
}
package com.shinonometn.music.server.build.document

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

open class MusicServerDocumentTask : DefaultTask() {

    internal val scannerList = mutableListOf<MusicServerDocumentExtension.ScannerTask>()

    private val outputDirectory = File(project.buildDir, "document/music-server")

    init {
        require(outputDirectory.exists() || outputDirectory.mkdirs()) { "Could not create output directory '${outputDirectory.absolutePath}'" }
    }

    @TaskAction
    fun generate() {
        scannerList.stream().parallel().forEach { handleScanning(it) }
    }

    private fun handleScanning(task : MusicServerDocumentExtension.ScannerTask) {
        val directories = task.directories.map { File(project.projectDir, it) }.filter { it.isDirectory }
        if (directories.isEmpty()) {
            logger.warn("No directory for handler {}.", task.handler::class.simpleName)
            return
        }

        logger.info("{} targets for {}.", directories.size, task.handler::class.simpleName)
        val files = mutableListOf<File>()
        val handler = task.handler
        for(directory in directories) {
            val walker = directory.walk()
            walker.forEach { if(it.isFile && handler.isFileSupported(it) ) files.add(it) }
        }
        if(files.isEmpty()) {
            logger.info("No file to handle.")
            return
        }
        logger.lifecycle("Will scanning {} files with {}.", files.size, handler::class.simpleName)
        val fragments = files.map { handler.parse(it) }.filter { it.isNotEmpty() }.flatten()
        fragments.forEach {
            println(it)
        }
    }

    companion object {
        const val TASK_NAME = "musicServerDocument"
    }
}
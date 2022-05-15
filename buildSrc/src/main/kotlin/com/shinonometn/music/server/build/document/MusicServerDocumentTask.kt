package com.shinonometn.music.server.build.document

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.util.*

open class MusicServerDocumentTask : DefaultTask() {

    internal val scannerList = mutableListOf<MusicServerDocumentExtension.ScannerTask>()

    private val json = ObjectMapper().apply {
        enable(SerializationFeature.INDENT_OUTPUT)
    }

    private val outputDirectory = File(project.buildDir, "document/music-server")
    private val fragmentDirectory = File(outputDirectory, "fragments")

    init {
        require(outputDirectory.exists() || outputDirectory.mkdirs()) {
            "Could not create output directory '${outputDirectory.absolutePath}'."
        }

        require(fragmentDirectory.exists() || fragmentDirectory.mkdirs()) {
            "Could not create temp fragment directory '{${fragmentDirectory.absolutePath}}'."
        }
    }

    @TaskAction
    fun generate() {
        logger.info("Cleaning up fragment directory.")
        fragmentDirectory.listFiles()?.forEach { it.delete() }
        if (scannerList.isEmpty()) {
            logger.warn(
                "No target to scan in ${project.name}. " +
                        "use `musicServerDocument { }` dsl to configure this plugin."
            )
            return
        }
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
        val outputFile = File(fragmentDirectory,
            "${handler::class.simpleName}-${UUID.randomUUID().toString().replace("-", "")}.json"
        )
        val output = FileOutputStream(outputFile)
        json.writeValue(output, fragments)
        output.flush()
        output.close()
        logger.info("Fragments were write to file.")
    }

    companion object {
        const val TASK_NAME = "musicServerDocument"
    }
}
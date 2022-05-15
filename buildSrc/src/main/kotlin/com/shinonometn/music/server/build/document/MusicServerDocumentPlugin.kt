package com.shinonometn.music.server.build.document

import org.gradle.api.Plugin
import org.gradle.api.Project

class MusicServerDocumentPlugin : Plugin<Project> {

    override fun apply(p0: Project) {
        p0.extensions.add(MusicServerDocumentExtension.MODAL_NAME, MusicServerDocumentExtension())
        addDocumentTask(p0)
    }

    private fun addDocumentTask(project: Project) {
        val logger = project.logger
        project.tasks.register(MusicServerDocumentTask.TASK_NAME, MusicServerDocumentTask::class.java) {
            it.group = TASK_GROUP
            val config = project.extensions.getByName(MusicServerDocumentExtension.MODAL_NAME) as MusicServerDocumentExtension

            if (config.scannerTasks.isEmpty()) {
                logger.warn(
                    "No target for scan. " +
                            "use `musicServerDocument { }` dsl to configure this plugin."
                )
                return@register
            }
            it.scannerList.addAll(config.scannerTasks)
        }
    }

    companion object {
        const val TASK_GROUP = "documentation"
    }
}
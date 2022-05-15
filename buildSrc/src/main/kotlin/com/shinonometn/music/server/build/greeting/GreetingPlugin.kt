package com.shinonometn.music.server.build.greeting

import org.gradle.api.Plugin
import org.gradle.api.Project

// https://riptutorial.com/gradle/example/6200/simple-gradle-plugin-from--buildsrc-
class GreetingPlugin : Plugin<Project> {
    override fun apply(p0: Project) {
        p0.extensions.add(GreetingModel.MODEL_NAME, GreetingModel())
        addGreetingTask(p0)
    }

    private fun addGreetingTask(project: Project) {
        project.tasks.register(GreetingTask.TASK_NAME, GreetingTask::class.java) {
            it.greeting = (project.extensions.getByName(GreetingModel.MODEL_NAME) as? GreetingModel)?.content ?: "Here is ${project.name}"
            it.group = TASK_GROUP
        }
    }

    companion object {
        const val TASK_GROUP = "documentation"
    }
}
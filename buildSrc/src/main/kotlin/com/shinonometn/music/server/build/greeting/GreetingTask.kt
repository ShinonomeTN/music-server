package com.shinonometn.music.server.build.greeting

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

// https://www.baeldung.com/gradle-custom-task
open class GreetingTask : DefaultTask() {
    @Input
    var greeting = "Hello this is a greeting task"

    @TaskAction
    fun greet() {
        println(greeting)
    }

    companion object {
        const val TASK_NAME = "greeting"
    }
}
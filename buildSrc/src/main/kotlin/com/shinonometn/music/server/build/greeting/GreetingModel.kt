package com.shinonometn.music.server.build.greeting

class GreetingModel {
    var content : String? = null

    override fun toString(): String {
        return "GreetingModel { content=${content} }"
    }

    companion object {
        const val MODEL_NAME = "greeting"
    }
}
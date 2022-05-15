package com.shinonometn.music.server.build.document.handler

import java.io.File

object JavaDocFileHandler : FileHandler {

    private val supportedFileExtensions = setOf("kt", "java")
    override fun isFileSupported(file: File) : Boolean {
        return file.extension in supportedFileExtensions
    }

    // something like `/* @restful_api_doc`
    private val startPattern = Regex("^.*?/\\*\\*?\\s*?@([A-Za-z\\d_-]+?)\\s*$")

    // `*/`
    private val endPattern = Regex("^.*?\\*/\\s*$")

    override fun parse(file: File): Collection<DocFragment> {
        require(file.isFile) { "'${file.absoluteFile}' is not a file." }

        val fragments = mutableListOf<DocFragment>()

        file.useLines {
            var title = ""
            var titleLineNumber = 0
            var lines = mutableListOf<String>()
            var entered = false
            var lineNumber = 0

            for(line in it) {
                lineNumber++
                if(entered) {
                    if(line.matches(endPattern)) {
                        entered = false
                        fragments.add(DocFragment(
                            file.absolutePath,
                            titleLineNumber,
                            title,
                            lines
                        ))
                        lines = mutableListOf()
                        continue
                    }

                    lines.add(line.trim().removePrefix("*").trim())
                    continue
                }
                val matched = startPattern.matchEntire(line) ?: continue
                entered = true
                title = matched.groupValues[1]
                titleLineNumber = lineNumber
            }
        }
        return fragments
    }
}
package com.shinonometn.music.server.build.document.handler

data class DocFragment(val file : String, val lineNumber : Int, val title :String, val lines : List<String>)
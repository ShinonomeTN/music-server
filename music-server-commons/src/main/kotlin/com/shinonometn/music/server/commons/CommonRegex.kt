package com.shinonometn.music.server.commons

import com.shinonometn.koemans.utils.CommonRegex

val CommonRegex.Server by lazy {
    Regex("^(([A-Za-z0-9_\\-]+\\.)+?([A-Za-z0-9_\\-]))|([A-Za-z0-9_-]+?)(:\\d+)?$")
}
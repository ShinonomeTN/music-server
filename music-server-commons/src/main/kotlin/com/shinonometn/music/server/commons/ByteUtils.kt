package com.shinonometn.music.server.commons

import org.apache.commons.codec.binary.Hex

fun Long.toByteArray() : ByteArray {
    return byteArrayOf(
            (this shr 56).toByte(),
            (this shr 48).toByte(),
            (this shr 40).toByte(),
            (this shr 32).toByte(),
            (this shr 24).toByte(),
            (this shr 16).toByte(),
            (this shr 8).toByte(),
            this.toByte()
    )
}
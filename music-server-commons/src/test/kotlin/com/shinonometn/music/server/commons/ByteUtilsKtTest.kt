package com.shinonometn.music.server.commons

import org.apache.commons.codec.binary.Hex
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class ByteUtilsKtTest {

    @Test
    fun `Test long to byte`() {
        val long = 0x1234567890ABCDEF
        val bytes = long.toByteArray()
        assertTrue(bytes.contentEquals(byteArrayOf(
                0x12, 0x34, 0x56, 0x78,
                0x90.toByte(),
                0xAB.toByte(),
                0xCD.toByte(),
                0xEF.toByte()
        )))
        println(Hex.encodeHexString(bytes))
    }
}
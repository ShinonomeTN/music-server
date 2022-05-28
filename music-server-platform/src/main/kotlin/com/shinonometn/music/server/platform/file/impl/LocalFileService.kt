package com.shinonometn.music.server.platform.file.impl

import com.shinonometn.music.server.commons.copyTo
import com.shinonometn.music.server.platform.file.PlatformFileInfo
import com.shinonometn.music.server.platform.file.PlatformFileService
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.locks.ReentrantLock

class LocalFileService(configuration: Configuration.() -> Unit) : PlatformFileService {

    private val storageLocation: String
    private val subPath: String
    private val host: String

    private val folderCreateLock = ReentrantLock()

    init {
        val config = Configuration().apply(configuration)
        storageLocation = config.localStorageLocation ?: error("'localStorageLocation' is required")
        subPath = config.path ?: "/"
        host = config.host ?: error("'host' is required")

        val file = File(storageLocation)
        if (!file.exists() && !file.mkdirs()) error("Could not create storage directory '${file.absoluteFile}'.")
        if (!file.isDirectory || !file.canWrite()) error("'${file.absoluteFile}' is not a directory or cannot be write.")
    }

    class Configuration {
        var host: String? = null
        var path: String? = null
        var localStorageLocation: String? = null
    }

    // Generate a filename base on sha256
    private fun generateFilePath(file: File, filename: String): Path {
        val hash = Base64.encodeBase64URLSafeString(
            file.inputStream().use { DigestUtils.sha256(it) } + file.inputStream().use { DigestUtils.md5(it) }
        )
        val newFilename = "${hash}${filename.substring(filename.lastIndexOf("."))}"
        val folder = hash.subSequence(0, 2).toString()
        return Paths.get(folder, newFilename)
    }

    override fun store(inputStream: InputStream, originFilename: String): PlatformFileInfo {
        val temp = Files.createTempFile("tmp-msfu", null).toFile()
        temp.deleteOnExit()
        temp.outputStream().use { inputStream.copyTo(it) }

        val filePath = generateFilePath(temp, originFilename)

        val localFile = Paths.get(storageLocation, filePath.toString()).toFile()
        if (!localFile.parentFile.exists()) {
            folderCreateLock.lock()
            try {
                require(localFile.parentFile.mkdirs()) { "Could not create directory '${localFile.parentFile.absoluteFile}'." }
            } finally {
                folderCreateLock.unlock()
            }
        }

        temp.copyTo(localFile)
        temp.delete()

        return PlatformFileInfo(
            Paths.get(host, subPath, filePath.toString()).toString(),
            filePath.toString(),
            localFile.length(),
            this
        )
    }

    override fun get(suffix: String): PlatformFileInfo {
        val file = File(storageLocation, suffix)
        return PlatformFileInfo(
            Paths.get(host, subPath, suffix).toString(),
            File(storageLocation, suffix).absolutePath,
            file.length(),
            this
        )
    }

    override fun delete(suffix: String): Boolean {
        val file = File(storageLocation, suffix)
        return file.delete()
    }

    override fun inputStreamOf(file: PlatformFileInfo): InputStream? {
        val f = File(file.internalPath).takeIf { it.isFile && it.exists() } ?: return null
        return f.inputStream()
    }
}
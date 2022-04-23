package com.shinonometn.music.server.service

import com.shinonometn.music.server.commons.toByteArray
import com.shinonometn.music.server.configuration.DeploymentConfiguration
import com.shinonometn.music.server.configuration.StorageConfiguration
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.IOUtils
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
class FileService(
    deploymentConfiguration: DeploymentConfiguration,
    storageConfiguration: StorageConfiguration
) {
    private val host = deploymentConfiguration.host
    private val location = File(storageConfiguration.path).absoluteFile
    private val subPath = storageConfiguration.subPath

    init {
        if(!location.exists() && !location.mkdirs()) {
            error("Could not create storage directory: ${location.absolutePath}")
        }
    }

    private fun storageLocationOf(subPath : String) : File {
        return File(location, subPath).absoluteFile
    }

    private fun generateFilePath(inputString: InputStream, filename : String): Path {
        val sha256 = DigestUtils.sha256Hex(inputString)
        val datetime = Hex.encodeHexString(System.currentTimeMillis().toByteArray())
        val newFilename = "${sha256}_$datetime${filename.substring(filename.lastIndexOf("."))}"
        val folder = sha256.subSequence(0, 2).toString()
        return Paths.get(folder, newFilename)
    }

    class FileInfo(val externalPath: String, val internalPath : String, val size: Long)

    fun store(inputStream: InputStream, originFilename: String): FileInfo {
        val tempFile = Files.createTempFile("tmp-msfu", null).toFile()
        tempFile.deleteOnExit()
        tempFile.outputStream().use { inputStream.copyTo(it) }

        val filePath = tempFile.inputStream().use { generateFilePath(it, originFilename) }

        val localPath = Paths.get(location.absolutePath, filePath.toString()).toFile()
        if(!localPath.parentFile.exists() && !localPath.parentFile.mkdirs()) error("Could not create directory '${localPath.parentFile.absoluteFile}'.")

        localPath.outputStream().use { output ->
            tempFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        tempFile.delete()
        val path = Paths.get(host, subPath, filePath.toString())

        return FileInfo(path.toString(), filePath.toString(), localPath.length())
    }

    fun delete(filePath: String) {
        val file = File(filePath)
        if(file.exists()) {
            file.delete()
        }
    }

    fun getFile(path: String): File {
        return File(location, path)
    }
}
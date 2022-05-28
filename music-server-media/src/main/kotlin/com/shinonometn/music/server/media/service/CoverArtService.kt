package com.shinonometn.music.server.media.service

import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.media.common.ImageOperation
import com.shinonometn.music.server.media.data.CoverArtData
import com.shinonometn.music.server.media.event.CoverArtDeleteEvent
import com.shinonometn.music.server.platform.file.PlatformFileService
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.awt.image.BufferedImage
import java.io.InputStream
import javax.imageio.ImageIO

@Service
class CoverArtService(
    private val fileService: PlatformFileService,
    private val database: SqlDatabase,
    private val eventPublisher: ApplicationEventPublisher
) {

    fun store(inputStream: InputStream, fileName: String): CoverArtData.Bean {
        val file = fileService.store(inputStream, fileName)
        return database {
            CoverArtData.Bean(CoverArtData.Entity.new {
                this.filePath = file.internalPath
            })
        }
    }

    fun get(path: String): InputStream? {
        return fileService.get(path).inputStream()
    }

    fun get(path: String, imageOperation: ImageOperation.() -> Unit): BufferedImage? {
        val inputStream = get(path) ?: return null
        val image = ImageIO.read(inputStream) ?: return null

        val op = ImageOperation(image, imageOperation)
        return op.toImage()
    }

    fun deleteById(id: Long) = database {
        val coverArt = CoverArtData.Entity.findById(id) ?: return@database 0
        coverArt.delete()
        fileService.delete(coverArt.filePath)
        eventPublisher.publishEvent(CoverArtDeleteEvent(this, id))
        1
    }

    fun findAll(paging: PageRequest, sorting: SortRequest): Page<CoverArtData.Bean> {
        return database {
            CoverArtData.findAll(paging, sorting)
        }
    }
}
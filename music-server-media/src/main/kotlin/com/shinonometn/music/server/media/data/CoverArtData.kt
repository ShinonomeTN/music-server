package com.shinonometn.music.server.media.data

import com.shinonometn.koemans.exposed.Page
import com.shinonometn.koemans.exposed.PageRequest
import com.shinonometn.koemans.exposed.pagingBy
import com.shinonometn.music.server.media.data.AlbumData.Table.clientDefault
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDateTime

object CoverArtData {
    fun findAll(paging: PageRequest): Page<Bean> {
        return Table.selectAll().pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    fun deleteById(id: Long): Int {
        return Table.deleteWhere { Table.id eq id }
    }


    object Table : LongIdTable("tb_art_cover") {
        val colFilePath = varchar("file_path", 255)
        val colCreateDate = datetime("create_date").clientDefault { LocalDateTime.now() }
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)
        var filePath by Table.colFilePath
        val createDate by Table.colCreateDate
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val filePath = entity.filePath
        val createDate = entity.createDate
    }
}
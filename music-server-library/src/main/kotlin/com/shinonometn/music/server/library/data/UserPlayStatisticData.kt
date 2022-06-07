package com.shinonometn.music.server.library.data

import com.shinonometn.music.server.platform.security.data.UserData
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserPlayStatisticData {
    object Table : LongIdTable("tb_library_stat") {
        val colUserId = reference("user_id", UserData.Table)
        val colMediaType = varchar("media_type", 128).index("i_ups_media")
        val colMediaRef = long("media_ref").index("i_ups_media_ref")
        val colActionType = varchar("action_type", 128).index("i_ups_action")
        val colCount = long("count")
        val colCreateDate = datetime("create_date").clientDefault { LocalDateTime.now() }
        val colUpdateDate = datetime("update_date").clientDefault { LocalDateTime.now() }
    }

    class Entity(id : EntityID<Long>) : LongEntity(id) {
        companion object: EntityClass<Long, Entity>(Table)
        var userId by Table.colUserId
        var mediaType by Table.colMediaType
        var actionType by Table.colActionType
        var count by Table.colCount
        var mediaRef by Table.colMediaRef
        val createDate by Table.colCreateDate
        var updateDate by Table.colUpdateDate
    }

    class Bean(entity : Entity) {
        val id = entity.id.value
        val userId = entity.userId.value
        val mediaType = entity.mediaType
        val mediaRef = entity.mediaRef
        val actionType = entity.actionType
        val count = entity.count
        val createDate = entity.createDate
        val updateDate = entity.updateDate
    }
}
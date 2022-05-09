package com.shinonometn.music.server.media.data

import com.shinonometn.koemans.exposed.*
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.minus
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.statements.UpdateStatement
import kotlin.math.abs

object PlaylistItemData {
    fun deleteByPlayListId(playlistId: Long): Int {
        return Table.deleteWhere { Table.colPlayListId eq playlistId }
    }

    fun rebasePlaylistOrders(playlistId: Long): Boolean {
        val minOrder = Table.colOrder.min()
        val minimumOrder = Table.slice(minOrder).select { Table.colPlayListId eq playlistId }.single()[minOrder] ?: 0
        if (minimumOrder == 0L) return false

        val updates = if (minimumOrder > 0L) {
            Table.updateByPlayList(playlistId, { Op.TRUE }) {
                it[colOrder] = colOrder - minimumOrder
            }
        } else {
            Table.updateByPlayList(playlistId, { Op.TRUE }) {
                it[colOrder] = colOrder + abs(minimumOrder)
            }
        }

        return updates > 0
    }

    fun moveItemAboveOf(playlistId: Long, itemId: Long, targetItemId: Long): Int {
        // First check both item exist
        val item = findItemByPlaylistIdAndId(playlistId, itemId) ?: return 0
        val targetItem = findItemByPlaylistIdAndId(playlistId, targetItemId) ?: return 0

        // Then, fetch elements that priority is greater or eq to target item ,take id and order.
        val preSelect = Table.selectByPlaylist(playlistId) {
            Table.colOrder greaterEq targetItem.order
        }.orderBy(Table.colOrder, SortOrder.DESC).limit(2).map {
            it[Table.id].value to it[Table.colOrder]
        }.takeIf { it.isNotEmpty() } ?: return 0

        // If the item above target item equals the moving one, skip
        if (preSelect.first().first == item.id) return 0

        // If only one element exists, means target element is on the top of the playlist.
        // Just set a larger order
        if (preSelect.size == 1) return updateOrder(itemId, preSelect.first().second + 1)

        var updateCount = 0

        val order = preSelect.first().second
        // take the element on top of target element, increase element orders by 2
        updateCount += Table.updateByPlayList(playlistId, { Table.colOrder greaterEq order }) {
            it[colOrder] = colOrder + 2
        }.takeIf { it > 0 } ?: return updateCount

        // Increase element order by 1 to insert the item
        updateCount += Table.updateByPlayList(playlistId, { Table.id eq itemId }) {
            it[colOrder] = order + 1
        }.takeIf { it > 0 } ?: return updateCount

        // Now target element should below of given element when sort by order decreasing.
        return updateCount
    }

    fun moveItemBelowOf(playlistId: Long, itemId: Long, targetItemId: Long): Int {
        // First check both item exist
        val item = findItemByPlaylistIdAndId(playlistId, itemId) ?: return 0
        val targetItem = findItemByPlaylistIdAndId(playlistId, targetItemId) ?: return 0

        // Then, fetch elements that priority is lesser or eq to target item , take id and order.
        val preSelect = Table.selectByPlaylist(playlistId) {
            Table.colOrder lessEq targetItem.order
        }.orderBy(Table.colOrder, SortOrder.ASC).limit(2).map {
            it[Table.id].value to it[Table.colOrder]
        }.takeIf { it.isNotEmpty() } ?: return 0

        // If only one element exists, means target element is under the bottom of the playlist.
        // Just set a smaller order
        if (preSelect.size == 1) return updateOrder(itemId, preSelect.first().second - 1)

        var updateCount = 0

        val order = preSelect[1].second
        // take the element on top of target element, decrease element orders by 2
        updateCount += Table.updateByPlayList(playlistId, { Table.colOrder lessEq order }) {
            it[colOrder] = colOrder - 2
        }.takeIf { it > 0 } ?: return updateCount

        // Decrease element order by 1 to insert the item
        updateCount += Table.updateByPlayList(playlistId, { Table.id eq itemId }) {
            it[colOrder] = order - 1
        }.takeIf { it > 0 } ?: return updateCount

        // Now target element should above of given element when sort by order decreasing.
        return updateCount
    }

    private fun updateOrder(itemId: Long, order: Long): Int {
        return Table.update({ Table.id eq itemId }) { it[colOrder] = order }
    }

    private fun findItemByPlaylistIdAndId(playlistId: Long, itemId: Long): Bean? {
        return Entity.find { (Table.colPlayListId eq playlistId) and (Table.id eq itemId) }.firstOrNull()?.let { Bean(it) }
    }

    fun reorder(playlistId: Long, itemId: Long, order: Long): Boolean {
        val playlistItem = Table.select { Table.colPlayListId eq playlistId and (Table.id eq itemId) }.singleOrNull() ?: return false

        val minOrder = Table.colOrder.min()
        val minimumOrder = Table.slice(minOrder).select { Table.colPlayListId eq playlistId }.single()[minOrder] ?: 0
        if (minimumOrder == 0L) return false

        val updates = Table.update({ Table.colPlayListId eq playlistId and (Table.id eq itemId) }) {
            it[colOrder] = colOrder - minimumOrder + order
        }

        return updates > 0
    }

    fun deletePlayListItem(playListId: Long, itemId: Long): Int {
        return Table.deleteWhere { (Table.colPlayListId eq playListId) and (Table.id eq itemId) }
    }

    fun findAll(playlistId: Long, paging: PageRequest, sorting: SortRequest): Page<Bean> {
        return Table.select { Table.colPlayListId eq playlistId }.orderBy(sorting).pagingBy(paging) {
            Bean(Entity.wrapRow(it))
        }
    }

    object Table : LongIdTable("tb_playlist_item") {
        val colPlayListId = long("playlist_id")
        val colTrackId = reference("track_id", TrackData.Table.id)
        val colOrder = long("order").default(0)

        fun selectByPlaylist(playlistId: Long, builder: SqlExpressionBuilder.() -> Op<Boolean>) = select {
            (colPlayListId eq playlistId) and builder()
        }

        fun updateByPlayList(playlistId: Long, builder: SqlExpressionBuilder.() -> Op<Boolean>, statement: Table.(UpdateStatement) -> Unit) =
            update({ colPlayListId eq playlistId and builder() }, body = statement)
    }

    class Entity(id: EntityID<Long>) : LongEntity(id) {
        companion object : LongEntityClass<Entity>(Table)

        var playlistId by Table.colPlayListId
        var order by Table.colOrder
        var trackId by Table.colTrackId
        val track by TrackData.Entity referencedOn Table.colTrackId
    }

    class Bean(entity: Entity) {
        val id = entity.id.value
        val playlistId = entity.playlistId
        val trackId = entity.trackId.value
        val order = entity.order
        val track = TrackData.Bean(entity.track)
    }
}
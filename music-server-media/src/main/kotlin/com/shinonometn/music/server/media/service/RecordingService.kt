package com.shinonometn.music.server.media.service

import com.shinonometn.koemans.exposed.FilterRequest
import com.shinonometn.koemans.exposed.SortRequest
import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.media.data.RecordingData
import com.shinonometn.music.server.media.data.TrackData
import org.jetbrains.exposed.dao.id.EntityID
import org.springframework.stereotype.Service

@Service
class RecordingService(private val database : SqlDatabase) {
    fun addTrackRecording(trackId: Long, protocol: String, server: String, location: String): RecordingData.Bean {
        return database {
            RecordingData.Bean(RecordingData.Entity.new {
                this.trackId = EntityID(trackId, TrackData.Table)
                this.protocol = protocol
                this.server = server
                this.location = location
            })
        }
    }

    fun updateTrackRecording(id: Long, protocol: String, server: String, location: String): RecordingData.Bean? {
        return database {
            val entity = RecordingData.Entity.findById(id) ?: return@database null
            entity.protocol = protocol
            entity.server = server
            entity.location = location
            RecordingData.Bean(entity)
        }
    }

    fun deleteTrackRecording(recordingId: Long): Boolean {
        return database { RecordingData.deleteById(recordingId) }
    }


    fun getRecordingsByTrackId(id: Long, filtering: FilterRequest, sorting: SortRequest): List<RecordingData.Bean> {
        return database {
            RecordingData.findAllByTrackId(id, filtering, sorting)
        }
    }
}
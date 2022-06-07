package com.shinonometn.music.server.platform.settings.configuration

import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.platform.PlatformInitAction
import com.shinonometn.music.server.platform.configuration.MetaConfiguration
import com.shinonometn.music.server.platform.settings.PlatformSetting
import com.shinonometn.music.server.platform.settings.data.SettingPropertyData
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.stereotype.Component

@Component
class SettingServiceInit(
    private val database : SqlDatabase,
    private val meta: MetaConfiguration,
    private val setting : PlatformSetting
) : PlatformInitAction() {
    override fun init() {
        database { SchemaUtils.createMissingTablesAndColumns(SettingPropertyData.Table) }

        setting.instanceGreeting ?: run { setting.instanceGreeting = meta.greeting }
        setting.allowGuest ?: run { setting.allowGuest = meta.allowGuest }
        setting.allowGuestRecordingAccess ?: run { setting.allowGuestRecordingAccess = meta.allowGuestRecordingAccess }
        setting.instanceName ?: run { setting.instanceName = meta.name }
        setting.instanceDescription ?: run { setting.instanceDescription = meta.description}
    }
}
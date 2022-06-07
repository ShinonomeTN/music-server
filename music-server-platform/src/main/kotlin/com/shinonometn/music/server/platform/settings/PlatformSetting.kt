package com.shinonometn.music.server.platform.settings

import org.springframework.stereotype.Component

@Component
class PlatformSetting(settingService: SettingService) {
    //
    // Server descriptions
    //
    var instanceName : String? by settingService["server.instance_name"]
    var instanceDescription : String? by settingService["server.instance_description"]
    var instanceGreeting : String? by settingService["server.instance_greeting"]

    //
    // Security
    //
    var allowGuest : Boolean? by settingService["security.allow_guest"]
    var allowGuestRecordingAccess : Boolean? by settingService["security.allow_guest_recording_access"]
    var robotsTxt : String? by settingService["security.robots.txt"]

    //
    // Appearances
    //
    var favicon : ByteArray? by settingService["appearance.favicon"]
}
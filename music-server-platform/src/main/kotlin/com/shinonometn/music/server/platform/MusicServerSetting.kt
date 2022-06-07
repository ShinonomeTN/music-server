package com.shinonometn.music.server.platform

import com.shinonometn.music.server.platform.common.FreemarkerExtension
import com.shinonometn.music.server.platform.configuration.MetaConfiguration
import com.shinonometn.music.server.platform.security.service.SecurityService
import com.shinonometn.music.server.platform.settings.PlatformSetting
import freemarker.template.TemplateMethodModelEx
import freemarker.template.utility.TemplateModelUtils
import org.springframework.stereotype.Component

/** @restful_api_param_doc
 * @bean_name MusicServerSetting
 * # Public settings of this server
 * | field name              | type           | required | description |
 * | -----------             | -------        | -------- | ----------- |
 * |host                     | String         | true     | Main hostname of this server |
 * |protocol                 | String         | true     | Protocol to communicate to this server, http or https |
 * |allowGuest               | Boolean        | true     | Is this server allow guest access|
 * |allowGuestRecordingAccess| Boolean        | true     | Is this server allow guest accessing recording resources |
 * |apiScopes                | Array[Object]  | true     | Public Api Scopes |
 * |apiVersion               | String         | true     | Api version |
 * |name                     | String         | false    | Optional, Title of this server |
 * |description              | String         | false    | Optional, Description of this server |
 * |greeting                 | String         | false    | Optional, greeting to user |
 * |abilities                | Array[Object]  | false    | Optional, server abilities |
 */
@Component
@FreemarkerExtension("musicServerConfig")
class MusicServerSetting(
    private val meta: MetaConfiguration,
    private val setting: PlatformSetting,
    private val platform: Platform,
    private val security: SecurityService
) : TemplateMethodModelEx {
    val host: String
        get() = meta.resolveHostName()

    val protocol: String
        get() = meta.protocol

    val allowGuest: Boolean
        get() = setting.allowGuest ?: true

    val allowGuestRecordingAccess: Boolean
        get() = setting.allowGuestRecordingAccess ?: false

    val apiScopes : Map<String, Map<String,String>>
        get() = security.normalScopes.associate { it.scope to it.descriptions }

    val apiVersion = "1.0"

    val name : String?
        get() = setting.instanceName

    val description: String?
        get() = setting.instanceDescription

    val greeting: String?
        get() = setting.instanceGreeting

    val abilities : Map<String, Map<String, String>>
        get() = platform.abilities.values.associate { it.identity to mapOf("title" to it.title, "description" to it.description) }

    override fun exec(arguments: MutableList<Any?>?): Any {
        return this
    }
}
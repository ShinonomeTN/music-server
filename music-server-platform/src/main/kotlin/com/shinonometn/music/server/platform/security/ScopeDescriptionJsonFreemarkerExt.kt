package com.shinonometn.music.server.platform.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.shinonometn.music.server.platform.common.FreemarkerExtension
import com.shinonometn.music.server.platform.security.service.SecurityService
import freemarker.template.TemplateMethodModelEx

@FreemarkerExtension("scopeDescriptionsJson")
class ScopeDescriptionJsonFreemarkerExt(
    private val securityService: SecurityService,
    private val json: ObjectMapper
) : TemplateMethodModelEx {
    override fun exec(arguments: MutableList<Any?>?): Any {
        return json.writeValueAsString(securityService.allScopes.associate { it.scope to it.descriptions })
    }
}
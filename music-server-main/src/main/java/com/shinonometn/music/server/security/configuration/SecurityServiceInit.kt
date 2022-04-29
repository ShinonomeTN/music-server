package com.shinonometn.music.server.security.configuration

import com.shinonometn.koemans.exposed.database.SqlDatabase
import com.shinonometn.music.server.commons.PlatformInitAction
import com.shinonometn.music.server.security.commons.AC
import com.shinonometn.music.server.security.data.AppTokenData
import com.shinonometn.music.server.security.data.SessionData
import com.shinonometn.music.server.security.data.UserData
import com.shinonometn.music.server.security.service.UserService
import org.jetbrains.exposed.sql.SchemaUtils
import org.springframework.stereotype.Component

@Component
class SecurityServiceInit(private val database: SqlDatabase, private val userService: UserService) : PlatformInitAction() {
    override fun init() {
        database {
            SchemaUtils.createMissingTablesAndColumns(
                UserData.Table,
                AppTokenData.Table,
                SessionData.Table
            )

            if(UserData.userCount() == 0L) {
                userService.createUser("admin", "admin", "Administrator", null, null) {
                    AC.Constants.ROLE to array(AC.Constants.SUPER_ADMIN)
                }
            }
        }
    }
}
package com.shinonometn.music.server.security.api

import com.shinonometn.koemans.coroutine.background
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.utils.isBoolean
import com.shinonometn.koemans.web.Validator
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.businessError
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.security.commons.AC
import com.shinonometn.music.server.security.commons.accessControl
import com.shinonometn.music.server.security.service.UserService
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.springframework.stereotype.Controller

@Controller
@KtorRoute("/api/user")
class UserManagementApi(private val userService: UserService) {

    class UserUpdateRequest(params : Parameters) {
        init {
            validator.validate(params)
        }

        val nickname = params["nickname"]
        val email = params["email"]
        val avatar = params["avatar"]
        val enabled = params["enabled"]!!.toBoolean()

        companion object {
            val validator = Validator {
                optional("nickname") with isString { it.length <= 255 }
                "enabled" with isString { it.isBoolean() }
                optional("email") with isString { it.length <= 255 }
                optional("avatar") with isString { it.length <= 255 }
            }
        }
    }

    class UserCreateRequest(params: Parameters) {
        init {
            validator.validate(params)
        }

        val username = params["username"]!!
        val nickname = params["nickname"]
        val email = params["email"]
        val avatar = params["avatar"]
        val password = params["password"]!!

        companion object {
            val validator = UserUpdateRequest.validator.copy {
                "username" with isString { it.length <= 64 }
                "password" with isString { it.length <= 255 }
                exclude("enabled")
            }
        }
    }

    @KtorRoute
    fun Route.userManagement() = accessControl(AC.Scope.Admin.UserManagement) {
        get {
            val paging = call.receivePageRequest()
            val result = background {
                userService.findAll(paging).convert {
                    mapOf("user" to it)
                }
            }

            call.respond(result)
        }

        post {
            val request = UserCreateRequest(call.receiveParameters())
            val result = background {
                userService.createUser(request.username, request.password, request.nickname, request.email, request.avatar)
            }

            call.respond(mapOf("user" to result))
        }

        route("/{id}") {
            get {
                val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
                val result = background { userService.findById(id) } ?: businessError("user_not_found")
                call.respond(mapOf("user" to result))
            }

            post {
                val id = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_id")
                val params = UserUpdateRequest(call.receiveParameters())
                val result = background {
                    userService.update(id, params.nickname, params.email, params.avatar, params.enabled)
                }

                call.respond(mapOf("user" to result))
            }
        }
    }

}
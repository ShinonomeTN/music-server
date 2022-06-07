package com.shinonometn.music.server.library.api

import com.shinonometn.koemans.exposed.FilterOptionMapping
import com.shinonometn.koemans.exposed.SortOptionMapping
import com.shinonometn.koemans.receiveFilterOptions
import com.shinonometn.koemans.receivePageRequest
import com.shinonometn.koemans.receiveSortOptions
import com.shinonometn.koemans.web.spring.route.KtorRoute
import com.shinonometn.music.server.commons.CR
import com.shinonometn.music.server.commons.validationError
import com.shinonometn.music.server.library.LibraryScope
import com.shinonometn.music.server.library.data.UserFollowedArtistData
import com.shinonometn.music.server.library.service.ArtistActivityService
import com.shinonometn.music.server.media.data.ArtistData
import com.shinonometn.music.server.platform.security.commons.acUserIdentityNotNull
import com.shinonometn.music.server.platform.security.commons.accessControl
import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import org.jetbrains.exposed.sql.SortOrder
import org.springframework.stereotype.Controller
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Controller
@KtorRoute("/api/library")
class UserArtistActivityApi(private val service: ArtistActivityService) {

    private val artistFollowingSorting = SortOptionMapping {
        "update_date" associateTo ArtistData.Table.colUpdateDate defaultOrder SortOrder.DESC
        "following_date" associateTo UserFollowedArtistData.Table.colFollowDate defaultOrder SortOrder.DESC
    }

    @KtorRoute("/artist")
    fun Route.listFollowingArtists() = accessControl(LibraryScope.Artist) {
        get {
            val userId = call.acUserIdentityNotNull.userId
            val paging = call.receivePageRequest()
            val sorting = call.receiveSortOptions(artistFollowingSorting)
            call.respond(service.findAllBy(userId, sorting, paging).convert {
                mapOf(
                    "favourite" to it,
                    "artist" to it.artist
                )
            })
        }

        param("fetch_update") {
            val datetimeParser = DateTimeFormatter.ISO_DATE_TIME

            val filter = FilterOptionMapping {
                "update_date_start" means { ArtistData.Table.colUpdateDate greaterEq LocalDateTime.from(datetimeParser.parse(it.asString())) }
                "update_date_end" means { ArtistData.Table.colUpdateDate lessEq LocalDateTime.from(datetimeParser.parse(it.asString())) }
            }

            get {
                val userId = call.acUserIdentityNotNull.userId
                val filtering = call.receiveFilterOptions(filter)
                val paging = call.receivePageRequest()
                call.respond(service.fetchArtistUpdate(userId, filtering, paging).convert {
                    mapOf(
                        "favourite" to it,
                        "artist" to it.artist
                    )
                })
            }
        }
    }

    @KtorRoute("/artist")
    fun Route.artistFollowApi() = route("/{id}") {

        accessControl(LibraryScope.Artist) {
            val datetimeParser = DateTimeFormatter.ISO_DATE_TIME
            get {
                val artistId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_artist_id")
                val userId = call.acUserIdentityNotNull.userId
                val result = service.fetchArtistUpdateInfo(userId, artistId)
                call.respond(
                    mapOf(
                        "favourite" to result,
                        "artist" to result?.artist
                    )
                )
            }

            post {
                val userId = call.acUserIdentityNotNull.userId
                val artistId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_artist_id")
                val datetime = call.receiveParameters()["date"]?.let { LocalDateTime.from(datetimeParser.parse(it)) } ?: LocalDateTime.now()
                call.respond(service.commitUserArtistUpdateFetched(userId, artistId, datetime))
            }
        }

        param("follow") {
            accessControl(LibraryScope.ArtistFollow) {
                post {
                    val artistId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_artist_id")
                    val userId = call.acUserIdentityNotNull.userId
                    call.respond(CR.successOrFailed(service.followArtist(artistId, userId)))
                }
            }
        }

        param("unfollow") {
            accessControl(LibraryScope.ArtistUnfollow) {
                post {
                    val artistId = call.parameters["id"]?.toLongOrNull() ?: validationError("invalid_artist_id")
                    val userId = call.acUserIdentityNotNull.userId
                    call.respond(CR.successOrFailed(service.unfollowArtist(artistId, userId)))
                }
            }
        }
    }
}
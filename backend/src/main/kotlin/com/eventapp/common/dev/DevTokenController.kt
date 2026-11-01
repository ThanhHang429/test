package com.eventapp.common.dev

import com.eventapp.common.response.ApiResponse
import com.eventapp.common.security.JwtTokenProvider
import com.eventapp.identity.repository.RoleRepository
import com.eventapp.identity.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * CHỈ tồn tại ở profile dev: cấp JWT cho một userId để test đường xác thực thật
 * mà không cần triển khai đăng nhập (phần của module khác).
 */
@Profile("dev")
@Tag(name = "Dev helper", description = "Chỉ có ở profile dev")
@RestController
@RequestMapping("/api/v1/dev")
class DevTokenController(
    private val tokenProvider: JwtTokenProvider,
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository
) {

    data class DevTokenResponse(
        val userId: Long,
        val email: String?,
        val roles: List<String>,
        val token: String,
        val tokenType: String = "Bearer",
        val expiresInSeconds: Long
    )

    @Operation(summary = "Cấp JWT cho userId (dev)")
    @PostMapping("/token")
    fun token(@RequestParam userId: Long): ApiResponse<DevTokenResponse> {
        val user = userRepository.findById(userId).orElseThrow {
            com.eventapp.common.exception.NotFoundException(message = "Không có user id=$userId")
        }
        val roles = roleRepository.findRoleCodesByUserId(userId)
        return ApiResponse.ok(
            DevTokenResponse(
                userId = userId,
                email = user.email,
                roles = roles,
                token = tokenProvider.issue(userId, roles),
                expiresInSeconds = tokenProvider.ttlSeconds()
            )
        )
    }

    @Operation(summary = "Danh sách tài khoản seed để test")
    @GetMapping("/accounts")
    fun accounts(): ApiResponse<List<Map<String, Any?>>> =
        ApiResponse.ok(
            userRepository.findAll().sortedBy { it.userId }.map {
                mapOf(
                    "userId" to it.userId,
                    "email" to it.email,
                    "fullName" to it.fullName,
                    "roles" to roleRepository.findRoleCodesByUserId(it.userId!!)
                )
            }
        )
}

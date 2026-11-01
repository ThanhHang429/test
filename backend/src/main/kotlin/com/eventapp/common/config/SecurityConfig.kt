package com.eventapp.common.config

import com.eventapp.common.response.ApiError
import com.eventapp.common.response.ErrorEnvelope
import com.eventapp.common.security.JwtAuthFilter
import com.eventapp.common.web.CorrelationId
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Cấu hình bảo mật: stateless JWT, không CSRF, phân quyền ở cả URL và method.
 * BR-001: mọi kiểm tra quyền quan trọng nằm ở backend, client chỉ hiển thị.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { auth ->
                auth
                    // Công khai (SRS 3.1)
                    .requestMatchers(HttpMethod.GET, "/api/v1/events", "/api/v1/events/*").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/events/*/reviews", "/api/v1/events/*/reviews/summary").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/categories").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/recommendations/preview").permitAll()
                    // Hạ tầng
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error").permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                    // Dev helper (chỉ có bean khi profile dev)
                    .requestMatchers("/api/v1/dev/**").permitAll()
                    // Còn lại phải đăng nhập
                    .anyRequest().authenticated()
            }
            .exceptionHandling { handling ->
                handling.authenticationEntryPoint { _, response, _ ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Chưa đăng nhập hoặc phiên đã hết hạn")
                }
                handling.accessDeniedHandler { _, response, _ ->
                    writeError(response, HttpStatus.FORBIDDEN, "FORBIDDEN", "Bạn không có quyền thực hiện thao tác này")
                }
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    private fun writeError(
        response: jakarta.servlet.http.HttpServletResponse,
        status: HttpStatus,
        code: String,
        message: String
    ) {
        response.status = status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        val body = ErrorEnvelope(
            success = false,
            error = ApiError(code = code, message = message),
            correlationId = CorrelationId.current()
        )
        response.writer.write(objectMapper.writeValueAsString(body))
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = listOf("*")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf(CorrelationId.HEADER)
            allowCredentials = false
            maxAge = 3600
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}

package com.deuktemsiru.config

import com.deuktemsiru.security.JwtAuthenticationFilter
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    @Value("\${app.security.dev-endpoints-enabled:true}")
    private val devEndpointsEnabled: Boolean,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .headers { headers -> headers.frameOptions { it.disable() } }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                val publicMatchers = mutableListOf(
                    // v1 인증 엔드포인트 (카카오 로그인, 토큰 갱신)
                    "/api/v1/auth/kakao/login",
                    "/api/v1/auth/refresh",
                    // API 문서
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    // 정적 업로드 파일
                    "/uploads/**",
                )
                if (devEndpointsEnabled) {
                    publicMatchers += "/h2-console/**"
                }
                auth.requestMatchers(*publicMatchers.toTypedArray()).permitAll()
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
}

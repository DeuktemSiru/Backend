package com.deuktemsiru.controller

import com.deuktemsiru.dto.LoginRequest
import com.deuktemsiru.dto.LoginResponse
import com.deuktemsiru.dto.RegisterRequest
import com.deuktemsiru.dto.UserResponse
import com.deuktemsiru.security.AuthContext
import com.deuktemsiru.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController(
    private val userService: UserService,
    private val authContext: AuthContext,
) {

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody req: RegisterRequest): UserResponse =
        userService.register(req)

    @PostMapping("/auth/login")
    fun login(@RequestBody req: LoginRequest): LoginResponse =
        userService.login(req)

    @GetMapping("/users/{userId}")
    fun getUser(@PathVariable userId: Long): UserResponse {
        authContext.requireCurrentUserId(userId)
        return userService.getUser(userId)
    }
}

package com.deuktemsiru.service

import com.deuktemsiru.dto.LoginRequest
import com.deuktemsiru.dto.LoginResponse
import com.deuktemsiru.dto.RegisterRequest
import com.deuktemsiru.dto.UserResponse
import com.deuktemsiru.entity.User
import com.deuktemsiru.entity.UserGrade
import com.deuktemsiru.repository.UserRepository
import com.deuktemsiru.security.JwtService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {

    @Transactional
    fun register(req: RegisterRequest): UserResponse {
        require(!userRepository.existsByEmail(req.email)) { "이미 사용 중인 이메일입니다." }
        val user = User(
            email = req.email,
            nickname = req.nickname,
            passwordHash = requireNotNull(passwordEncoder.encode(req.password)),
            role = req.role,
        )
        return UserResponse.from(userRepository.save(user))
    }

    fun login(req: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(req.email)
            .orElseThrow { IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.") }
        require(passwordEncoder.matches(req.password, user.passwordHash)) { "이메일 또는 비밀번호가 올바르지 않습니다." }
        return LoginResponse(
            userId = user.id,
            nickname = user.nickname,
            role = user.role,
            token = jwtService.createToken(user),
        )
    }

    fun getUser(userId: Long): UserResponse {
        val user = findUser(userId)
        return UserResponse.from(user)
    }

    @Transactional
    fun updateSavings(userId: Long, savedAmount: Int) {
        val user = findUser(userId)
        user.totalSavings += savedAmount
        user.co2Saved += savedAmount * 0.00003f // 1원당 약 0.00003kg CO2 절감 추산
        user.grade = when {
            user.totalSavings >= 50_000 -> UserGrade.FOREST
            user.totalSavings >= 10_000 -> UserGrade.TREE
            else -> UserGrade.SPROUT
        }
    }

    fun findUser(userId: Long): User =
        userRepository.findById(userId).orElseThrow { NoSuchElementException("사용자를 찾을 수 없습니다.") }
}

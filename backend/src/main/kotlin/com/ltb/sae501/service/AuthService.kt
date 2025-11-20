package com.ltb.sae501.service

import com.ltb.sae501.entity.User
import com.ltb.sae501.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {
    @Transactional
    fun register(username: String, password: String, email: String?): User? {
        if (userRepository.existsByUsername(username)) {
            return null
        }

        val user = User(
            id = UUID.randomUUID().toString(),
            username = username,
            password = passwordEncoder.encode(password),
            email = email,
            createdAt = System.currentTimeMillis()
        )

        return userRepository.save(user)
    }

    fun authenticate(username: String, password: String): User? {
        val user = userRepository.findByUsername(username).orElse(null) ?: return null

        return if (passwordEncoder.matches(password, user.password)) {
            user.lastLogin = System.currentTimeMillis()
            userRepository.save(user)
            user
        } else {
            null
        }
    }

    fun getUserById(userId: String): User? {
        return userRepository.findById(userId).orElse(null)
    }

    fun getUserByUsername(username: String): User? {
        return userRepository.findByUsername(username).orElse(null)
    }
}

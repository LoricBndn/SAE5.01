package com.ltb.sae501.controller

import com.ltb.sae501.dto.AuthRequest
import com.ltb.sae501.dto.AuthResponse
import com.ltb.sae501.dto.RegisterRequest
import com.ltb.sae501.service.AuthService
import com.ltb.sae501.service.JwtService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = ["*"])
class AuthController(
    private val authService: AuthService,
    private val jwtService: JwtService
) {
    @PostMapping("/register")
    fun register(@RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> {
        val user = authService.register(request.username, request.password, request.email)
            ?: return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(AuthResponse(null, null, "Username already exists"))

        val token = jwtService.generateToken(user.id, user.username)

        return ResponseEntity.ok(AuthResponse(token, user.id, "Registration successful"))
    }

    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): ResponseEntity<AuthResponse> {
        val user = authService.authenticate(request.username, request.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(AuthResponse(null, null, "Invalid credentials"))

        val token = jwtService.generateToken(user.id, user.username)

        return ResponseEntity.ok(AuthResponse(token, user.id, "Login successful"))
    }

    @GetMapping("/validate")
    fun validateToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Map<String, Any>> {
        val token = authHeader.removePrefix("Bearer ")

        return if (jwtService.validateToken(token)) {
            val userId = jwtService.extractUserId(token)
            val username = jwtService.extractUsername(token)
            ResponseEntity.ok(mapOf<String, Any>("valid" to true, "userId" to (userId ?: ""), "username" to (username ?: "")))
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(mapOf<String, Any>("valid" to false, "message" to "Invalid or expired token"))
        }
    }
}

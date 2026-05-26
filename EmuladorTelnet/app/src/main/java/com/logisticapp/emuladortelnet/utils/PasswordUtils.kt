package com.logisticapp.emuladortelnet.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.*

/**
 * Utilitários para tratamento de senhas
 */
object PasswordUtils {
    
    /**
     * Gera hash SHA-256 de uma senha
     */
    fun hashPassword(password: String): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashBytes = messageDigest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Verifica se a senha corresponde ao hash
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }
    
    /**
     * Valida se uma senha é forte
     * Requisitos:
     * - Mínimo 6 caracteres
     * - Pelo menos uma letra maiúscula
     * - Pelo menos uma letra minúscula
     * - Pelo menos um número
     */
    fun isPasswordStrong(password: String): Boolean {
        return password.length >= 6 &&
                password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } &&
                password.any { it.isDigit() }
    }
    
    /**
     * Valida se o email está em formato correto
     */
    fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}".toRegex()
        return email.matches(emailPattern)
    }
}

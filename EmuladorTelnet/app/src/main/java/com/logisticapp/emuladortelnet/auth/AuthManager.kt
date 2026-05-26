package com.logisticapp.emuladortelnet.auth

import android.content.Context
import android.content.SharedPreferences
import com.logisticapp.emuladortelnet.database.User
import timber.log.Timber

/**
 * Gerenciador de autenticação e sessão do usuário
 */
class AuthManager(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    /**
     * Salva os dados de login do usuário
     */
    fun saveLoginSession(user: User) {
        sharedPreferences.edit().apply {
            putInt(KEY_USER_ID, user.id)
            putString(KEY_USER_EMAIL, user.email)
            putString(KEY_USER_NAME, user.fullName)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
        Timber.d("Sessão de login salva: ${user.email}")
    }
    
    /**
     * Verifica se o usuário está logado
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
    
    /**
     * Obtém o ID do usuário logado
     */
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }
    
    /**
     * Obtém o email do usuário logado
     */
    fun getUserEmail(): String {
        return sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
    }
    
    /**
     * Obtém o nome do usuário logado
     */
    fun getUserName(): String {
        return sharedPreferences.getString(KEY_USER_NAME, "") ?: ""
    }
    
    /**
     * Faz logout do usuário
     */
    fun logout() {
        sharedPreferences.edit().apply {
            remove(KEY_USER_ID)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
        Timber.d("Usuário fez logout")
    }
    
    /**
     * Limpa todos os dados de autenticação
     */
    fun clearAuthData() {
        sharedPreferences.edit().clear().apply()
        Timber.d("Dados de autenticação limpos")
    }
}

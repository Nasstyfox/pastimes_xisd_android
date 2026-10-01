package com.pastimes.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.model.User
import com.pastimes.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val repo = AuthRepository()
    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Email and password are required")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = repo.login(email.trim(), password)
                _state.value = AuthState.Success(user)
            } catch (e: Exception) {
                _state.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String
    ) {
        if (fullName.isBlank() || email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Name, email and password are required")
            return
        }
        if (password.length < 6) {
            _state.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        _state.value = AuthState.Loading
        viewModelScope.launch {
            try {
                val user = repo.register(
                    email = email.trim(),
                    password = password,
                    fullName = fullName.trim(),
                    phone = phone.trim().ifBlank { null },
                    role = role
                )
                _state.value = AuthState.Success(user)
            } catch (e: Exception) {
                _state.value = AuthState.Error(friendlyError(e.message))
            }
        }
    }

    fun reset() {
        _state.value = AuthState.Idle
    }

    private fun friendlyError(raw: String?): String {
        val msg = raw ?: return "Something went wrong"
        return when {
            msg.contains("password is invalid", true) -> "Incorrect email or password"
            msg.contains("no user record", true) -> "No account found with that email"
            msg.contains("email address is already in use", true) -> "That email is already registered"
            msg.contains("network error", true) -> "Network error. Check your connection"
            else -> msg
        }
    }
}
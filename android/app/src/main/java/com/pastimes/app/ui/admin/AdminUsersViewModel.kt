package com.pastimes.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.AdminUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminUsersUiState(
    val users: List<AdminUser> = emptyList(),
    val filter: String = "all",   // all | buyer | seller | admin
    val loading: Boolean = false,
    val error: String? = null
)

class AdminUsersViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(AdminUsersUiState())
    val state: StateFlow<AdminUsersUiState> = _state

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val roleFilter = if (_state.value.filter == "all") null else _state.value.filter
                val list = api.adminUsers(roleFilter)
                _state.value = _state.value.copy(users = list, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load users"
                )
            }
        }
    }

    fun setFilter(filter: String) {
        _state.value = _state.value.copy(filter = filter)
        load()
    }
}
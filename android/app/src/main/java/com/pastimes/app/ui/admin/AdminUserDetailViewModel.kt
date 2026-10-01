package com.pastimes.app.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.AdminEarningsResponse
import com.pastimes.app.data.model.AdminTransaction
import com.pastimes.app.data.model.AdminUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminUserDetailUiState(
    val user: AdminUser? = null,
    val transactions: List<AdminTransaction> = emptyList(),
    val earnings: AdminEarningsResponse? = null,
    val loading: Boolean = false,
    val actionInProgress: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class AdminUserDetailViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(AdminUserDetailUiState())
    val state: StateFlow<AdminUserDetailUiState> = _state

    fun load(userId: Long) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val user = api.adminUserDetail(userId)
                var txns: List<AdminTransaction> = emptyList()
                var earn: AdminEarningsResponse? = null
                if (user.role == "buyer") {
                    txns = api.adminUserTransactions(userId)
                } else if (user.role == "seller") {
                    earn = api.adminUserEarnings(userId)
                }
                _state.value = _state.value.copy(
                    user = user,
                    transactions = txns,
                    earnings = earn,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load user"
                )
            }
        }
    }

    fun resetPassword() {
        val user = _state.value.user ?: return
        _state.value = _state.value.copy(actionInProgress = true, message = null, error = null)
        viewModelScope.launch {
            try {
                val resp = api.adminResetPassword(user.id)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    message = resp.message ?: resp.error ?: "Reset email sent"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    error = e.message ?: "Failed to reset password"
                )
            }
        }
    }

    fun toggleActive() {
        val user = _state.value.user ?: return
        _state.value = _state.value.copy(actionInProgress = true, message = null, error = null)
        viewModelScope.launch {
            try {
                val resp = api.adminToggleActive(user.id)
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    message = if (resp.isActive == 1) "User activated" else "User deactivated"
                )
                load(user.id)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    actionInProgress = false,
                    error = e.message ?: "Failed to toggle status"
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }
}
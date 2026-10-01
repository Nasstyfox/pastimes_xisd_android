package com.pastimes.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.SettingsRequest
import com.pastimes.app.data.model.User
import com.pastimes.app.data.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val user: User? = null,
    val settings: UserSettings? = null,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class SettingsViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val me = api.me()
                val settings = api.getSettings()
                _state.value = _state.value.copy(
                    user = me,
                    settings = settings,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load settings"
                )
            }
        }
    }

    fun update(
        theme: String? = null,
        notificationsEnabled: Boolean? = null,
        language: String? = null
    ) {
        _state.value = _state.value.copy(saving = true, message = null, error = null)
        viewModelScope.launch {
            try {
                val saved = api.updateSettings(
                    SettingsRequest(
                        theme = theme,
                        notificationsEnabled = notificationsEnabled,
                        language = language
                    )
                )
                _state.value = _state.value.copy(
                    settings = saved,
                    saving = false,
                    message = "Settings saved"
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    saving = false,
                    error = e.message ?: "Failed to save"
                )
            }
        }
    }

    fun clearMessage() {
        _state.value = _state.value.copy(message = null, error = null)
    }
}
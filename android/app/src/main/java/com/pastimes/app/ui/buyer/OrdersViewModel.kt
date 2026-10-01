package com.pastimes.app.ui.buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null
)

class OrdersViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val list = api.orders()
                _state.value = OrdersUiState(orders = list, loading = false)
            } catch (e: Exception) {
                _state.value = OrdersUiState(
                    loading = false,
                    error = e.message ?: "Failed to load orders"
                )
            }
        }
    }
}
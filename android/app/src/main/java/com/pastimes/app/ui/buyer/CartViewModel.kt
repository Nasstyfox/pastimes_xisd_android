package com.pastimes.app.ui.buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.CartResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CartUiState(
    val cart: CartResponse? = null,
    val loading: Boolean = false,
    val error: String? = null
)

class CartViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(CartUiState())
    val state: StateFlow<CartUiState> = _state

    fun load() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val c = api.cart()
                _state.value = CartUiState(cart = c, loading = false)
            } catch (e: Exception) {
                _state.value = CartUiState(
                    loading = false,
                    error = e.message ?: "Failed to load cart"
                )
            }
        }
    }

    fun remove(itemId: Long) {
        viewModelScope.launch {
            try {
                api.removeFromCart(itemId)
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun clear() {
        viewModelScope.launch {
            try {
                api.clearCart()
                load()
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}
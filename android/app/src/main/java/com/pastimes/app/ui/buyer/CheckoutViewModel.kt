package com.pastimes.app.ui.buyer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.Address
import com.pastimes.app.data.model.AddressRequest
import com.pastimes.app.data.model.CreateOrderRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CheckoutUiState(
    val addresses: List<Address> = emptyList(),
    val selectedAddressId: Long? = null,
    val loading: Boolean = false,
    val placing: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

class CheckoutViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state

    init { loadAddresses() }

    fun loadAddresses() {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val list = api.addresses()
                val default = list.firstOrNull { it.isDefault == 1 }?.id
                    ?: list.firstOrNull()?.id
                _state.value = _state.value.copy(
                    addresses = list,
                    selectedAddressId = _state.value.selectedAddressId ?: default,
                    loading = false
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load addresses"
                )
            }
        }
    }

    fun select(id: Long) {
        _state.value = _state.value.copy(selectedAddressId = id)
    }

    fun addAddress(req: AddressRequest, onAdded: () -> Unit = {}) {
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            try {
                val created = api.createAddress(req)
                val list = _state.value.addresses + created
                _state.value = _state.value.copy(
                    addresses = list,
                    selectedAddressId = created.id,
                    loading = false
                )
                onAdded()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to add address"
                )
            }
        }
    }

    fun placeOrder() {
        val addrId = _state.value.selectedAddressId ?: run {
            _state.value = _state.value.copy(error = "Please select a delivery address")
            return
        }
        _state.value = _state.value.copy(placing = true, error = null)
        viewModelScope.launch {
            try {
                api.createOrder(CreateOrderRequest(addrId))
                _state.value = _state.value.copy(placing = false, success = true)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    placing = false,
                    error = e.message ?: "Failed to place order"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
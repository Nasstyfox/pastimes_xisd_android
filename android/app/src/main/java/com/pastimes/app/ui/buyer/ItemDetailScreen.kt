package com.pastimes.app.ui.buyer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.AddToCartRequest
import com.pastimes.app.data.model.Item
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ItemDetailState(
    val item: Item? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val adding: Boolean = false,
    val addedMessage: String? = null
)

class ItemDetailViewModel : ViewModel() {
    private val api = ApiClient.retrofit.create(ApiService::class.java)
    private val _state = MutableStateFlow(ItemDetailState())
    val state: StateFlow<ItemDetailState> = _state

    fun load(id: Long) {
        viewModelScope.launch {
            try {
                val item = api.itemDetail(id)
                _state.value = _state.value.copy(item = item, loading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    loading = false,
                    error = e.message ?: "Failed to load"
                )
            }
        }
    }

    fun addToCart() {
        val item = _state.value.item ?: return
        _state.value = _state.value.copy(adding = true, addedMessage = null)
        viewModelScope.launch {
            try {
                val resp = api.addToCart(AddToCartRequest(item.id))
                _state.value = _state.value.copy(
                    adding = false,
                    addedMessage = if (resp.ok == true) "Added to cart" else (resp.error ?: "Failed")
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    adding = false,
                    addedMessage = e.message ?: "Failed to add"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    viewModel: ItemDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(itemId) { viewModel.load(itemId) }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.addedMessage) {
        state.addedMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.let { /* leave message visible */ }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.item != null -> {
                    val item = state.item!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        item.imageUrl?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxWidth().height(300.dp)
                            )
                        }
                        Column(Modifier.padding(20.dp)) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "R %.2f".format(item.price),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item.size?.let { InfoChip("Size: $it") }
                                item.conditionTag.let { InfoChip("Condition: $it") }
                            }
                            item.brand?.let {
                                Spacer(Modifier.height(6.dp))
                                Text("Brand: $it", style = MaterialTheme.typography.bodyMedium)
                            }
                            item.colour?.let {
                                Text("Colour: $it", style = MaterialTheme.typography.bodyMedium)
                            }
                            item.sellerName?.let {
                                Spacer(Modifier.height(6.dp))
                                Text("Seller: $it", style = MaterialTheme.typography.bodyMedium)
                            }

                            item.description?.let {
                                Spacer(Modifier.height(16.dp))
                                Text(it, style = MaterialTheme.typography.bodyMedium)
                            }

                            Spacer(Modifier.height(28.dp))

                            Button(
                                onClick = { viewModel.addToCart() },
                                enabled = !state.adding && item.status == "available",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (state.adding) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text(
                                        if (item.status == "available") "Add to Cart"
                                        else "Unavailable (${item.status})"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium
        )
    }
}
package com.pastimes.app.ui.buyer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pastimes.app.data.model.Address
import com.pastimes.app.data.model.AddressRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onOrderPlaced: () -> Unit,
    viewModel: CheckoutViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddForm by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.success) {
        if (state.success) onOrderPlaced()
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Delivery Address", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))

            if (state.loading && state.addresses.isEmpty()) {
                CircularProgressIndicator()
            } else if (state.addresses.isEmpty()) {
                Text(
                    "No saved addresses yet.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                state.addresses.forEach { addr ->
                    AddressRow(
                        addr = addr,
                        selected = state.selectedAddressId == addr.id,
                        onSelect = { viewModel.select(addr.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showAddForm = !showAddForm },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (showAddForm) "Cancel" else "Add New Address")
            }

            if (showAddForm) {
                Spacer(Modifier.height(12.dp))
                AddressForm(
                    onSave = { req ->
                        viewModel.addAddress(req) { showAddForm = false }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.placeOrder() },
                enabled = !state.placing && state.selectedAddressId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.placing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Place Order")
                }
            }
        }
    }
}

@Composable
private fun AddressRow(addr: Address, selected: Boolean, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onSelect)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(addr.recipient, style = MaterialTheme.typography.titleSmall)
                Text("${addr.street}, ${addr.suburb}", style = MaterialTheme.typography.bodySmall)
                Text("${addr.city}, ${addr.province} ${addr.postalCode}",
                    style = MaterialTheme.typography.bodySmall)
                Text(addr.phone, style = MaterialTheme.typography.bodySmall)
                if (addr.isDefault == 1) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "Default",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressForm(onSave: (AddressRequest) -> Unit) {
    var label by remember { mutableStateOf("") }
    var recipient by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var suburb by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var province by remember { mutableStateOf("") }
    var postal by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = recipient, onValueChange = { recipient = it },
            label = { Text("Recipient name *") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("Phone *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = street, onValueChange = { street = it },
            label = { Text("Street address *") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = suburb, onValueChange = { suburb = it },
            label = { Text("Suburb *") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = city, onValueChange = { city = it },
            label = { Text("City *") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = province, onValueChange = { province = it },
            label = { Text("Province *") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = postal, onValueChange = { postal = it },
            label = { Text("Postal code *") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = label, onValueChange = { label = it },
            label = { Text("Label (e.g. Home)") },
            singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
            Text("Set as default")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                onSave(
                    AddressRequest(
                        label = label.ifBlank { null },
                        recipient = recipient.trim(),
                        phone = phone.trim(),
                        street = street.trim(),
                        suburb = suburb.trim(),
                        city = city.trim(),
                        province = province.trim(),
                        postalCode = postal.trim(),
                        isDefault = isDefault
                    )
                )
            },
            enabled = recipient.isNotBlank() && phone.isNotBlank() &&
                    street.isNotBlank() && suburb.isNotBlank() &&
                    city.isNotBlank() && province.isNotBlank() &&
                    postal.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save Address") }
    }
}
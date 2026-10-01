package com.pastimes.app.ui.seller

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.engage.common.datamodel.Image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    editItemId: Long? = null,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEditItemViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(editItemId) {
        if (editItemId != null) viewModel.loadForEdit(editItemId)
    }
    LaunchedEffect(state.success) {
        if (state.success) onSaved()
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbar.showSnackbar(it) }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.uploadImage(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editItemId == null) "Add Item" else "Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Image picker / preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { imageLauncher.launch("image/*") }
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (state.uploading) {
                        CircularProgressIndicator()
                    } else if (state.imageUrl != null) {
                        AsyncImage(
                            model = state.imageUrl,
                            contentDescription = "Item image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "📷",
                                style = MaterialTheme.typography.displaySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Tap to pick an image")
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Category dropdown
            CategoryDropdown(
                categories = state.categories,
                selectedId = state.categoryId,
                onSelected = { viewModel.setCategory(it) }
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = { Text("Title *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.price,
                onValueChange = viewModel::setPrice,
                label = { Text("Price (R) *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.size,
                    onValueChange = viewModel::setSize,
                    label = { Text("Size") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.brand,
                    onValueChange = viewModel::setBrand,
                    label = { Text("Brand") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.colour,
                onValueChange = viewModel::setColour,
                label = { Text("Colour") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text("Condition", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("new", "like_new", "good", "fair").forEach { c ->
                    FilterChip(
                        selected = state.condition == c,
                        onClick = { viewModel.setCondition(c) },
                        label = { Text(c.replace("_", " ")) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { viewModel.save() },
                enabled = !state.saving && !state.uploading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (editItemId == null) "Create Listing" else "Save Changes")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<com.pastimes.app.data.model.Category>,
    selectedId: Long?,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name
        ?: "Select category *"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = { Text(cat.name) },
                    onClick = {
                        onSelected(cat.id)
                        expanded = false
                    }
                )
            }
        }
    }
}
package com.pastimes.app.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailScreen(
    userId: Long,
    onBack: () -> Unit,
    viewModel: AdminUserDetailViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(userId) { viewModel.load(userId) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (state.loading && state.user == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val user = state.user ?: run {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("User not found")
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
            // ---- Profile card ----
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        user.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    InfoRow("Email", user.email)
                    InfoRow("Phone", user.phone ?: "—")
                    InfoRow("Role", user.role.replaceFirstChar { it.uppercase() })
                    InfoRow("Provider", user.authProvider)
                    InfoRow(
                        "Status",
                        if (user.isActive == 1) "Active" else "Inactive"
                    )
                    InfoRow("Joined", user.createdAt?.take(10) ?: "—")
                }
            }
            Spacer(Modifier.height(12.dp))

            // ---- Summary card ----
            state.user?.summary?.let { s ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (user.role == "buyer") "Purchase Summary" else "Sales Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        if (user.role == "buyer") {
                            InfoRow("Orders", "${s.orderCount ?: 0}")
                            InfoRow("Total Spent", "R %.2f".format(s.totalSpent ?: 0.0))
                        } else if (user.role == "seller") {
                            InfoRow("Items Listed", "${s.totalItems ?: 0}")
                            InfoRow("Available", "${s.availableItems ?: 0}")
                            InfoRow("Sold", "${s.soldItems ?: 0}")
                            InfoRow("Total Earnings", "R %.2f".format(s.totalEarnings ?: 0.0))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---- Transactions (buyer) ----
            if (user.role == "buyer" && state.transactions.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Transaction History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        state.transactions.forEach { txn ->
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Order #${txn.id}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "R %.2f".format(txn.totalAmount),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    txn.createdAt?.take(19)?.replace("T", " ") ?: "",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                txn.items.forEach { it ->
                                    Text(
                                        "  • ${it.title ?: "Item"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Divider()
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---- Sales / Earnings (seller) ----
            state.earnings?.let { e ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Sales History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Earned", fontWeight = FontWeight.SemiBold)
                            Text(
                                "R %.2f".format(e.totalEarnings),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "${e.soldCount} item(s) sold",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        e.sales.forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.title ?: "Item", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "to ${s.buyerName ?: "Buyer"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    "R %.2f".format(s.priceAtPurchase),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ---- Actions ----
            Text(
                "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { viewModel.resetPassword() },
                enabled = !state.actionInProgress,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.actionInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Reset Password (send email)")
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = { viewModel.toggleActive() },
                enabled = !state.actionInProgress && user.role != "admin",
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (user.isActive == 1) "Deactivate User" else "Activate User"
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
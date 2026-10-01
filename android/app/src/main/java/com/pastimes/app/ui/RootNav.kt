package com.pastimes.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.pastimes.app.ui.admin.AdminUserDetailScreen
import com.pastimes.app.ui.admin.AdminUsersScreen
import com.pastimes.app.ui.buyer.BuyerHomeScreen
import com.pastimes.app.ui.buyer.CartScreen
import com.pastimes.app.ui.buyer.CheckoutScreen
import com.pastimes.app.ui.buyer.ItemDetailScreen
import com.pastimes.app.ui.buyer.OrdersScreen
import com.pastimes.app.ui.seller.AddEditItemScreen
import com.pastimes.app.ui.seller.SellerDashboardScreen
import com.pastimes.app.ui.seller.SellerListingsScreen
import com.pastimes.app.ui.settings.SettingsScreen

data class BottomTab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun RootNav(role: String, onLogout: () -> Unit) {
    val navController = rememberNavController()

    val tabs: List<BottomTab> = when (role) {
        "buyer" -> listOf(
            BottomTab("home", "Home", Icons.Default.Home),
            BottomTab("cart", "Cart", Icons.Default.ShoppingCart),
            BottomTab("orders", "Orders", Icons.Default.List),
            BottomTab("settings", "Settings", Icons.Default.Settings)
        )
        "seller" -> listOf(
            BottomTab("home", "Dashboard", Icons.Default.Home),
            BottomTab("listings", "Listings", Icons.Default.List),
            BottomTab("settings", "Settings", Icons.Default.Settings)
        )
        "admin" -> listOf(
            BottomTab("home", "Users", Icons.Default.Person),
            BottomTab("settings", "Settings", Icons.Default.Settings)
        )
        else -> listOf(BottomTab("home", "Home", Icons.Default.Home))
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                when (role) {
                    "buyer" -> BuyerHomeScreen(
                        onItemClick = { id -> navController.navigate("item/$id") }
                    )
                    "seller" -> SellerDashboardScreen(
                        onViewListings = { navController.navigate("listings") }
                    )
                    "admin" -> AdminUsersScreen(
                        onUserClick = { id -> navController.navigate("admin-user/$id") }
                    )
                    else -> Placeholder("Unknown role")
                }
            }

            composable("item/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                ItemDetailScreen(
                    itemId = id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("cart") {
                CartScreen(onCheckout = { navController.navigate("checkout") })
            }

            composable("checkout") {
                CheckoutScreen(
                    onBack = { navController.popBackStack() },
                    onOrderPlaced = {
                        navController.navigate("orders") {
                            this.popUpTo("home")
                            this.launchSingleTop = true
                        }
                    }
                )
            }

            composable("orders") { OrdersScreen() }

            composable("listings") {
                SellerListingsScreen(
                    onAddItem = { navController.navigate("item-new") },
                    onEditItem = { id -> navController.navigate("item-edit/$id") }
                )
            }

            composable("item-new") {
                AddEditItemScreen(
                    editItemId = null,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate("listings") {
                            this.popUpTo("home")
                            this.launchSingleTop = true
                        }
                    }
                )
            }

            composable("item-edit/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                AddEditItemScreen(
                    editItemId = id,
                    onBack = { navController.popBackStack() },
                    onSaved = {
                        navController.navigate("listings") {
                            this.popUpTo("home")
                            this.launchSingleTop = true
                        }
                    }
                )
            }

            composable("admin-user/{id}") { entry ->
                val id = entry.arguments?.getString("id")?.toLongOrNull() ?: 0L
                AdminUserDetailScreen(
                    userId = id,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(onLogout = onLogout)
            }
        }
    }
}

@Composable
fun Placeholder(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text)
    }
}
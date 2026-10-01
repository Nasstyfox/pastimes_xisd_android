package com.pastimes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.ui.RootNav
import com.pastimes.app.ui.auth.LoginScreen
import com.pastimes.app.ui.auth.RegisterScreen
import com.pastimes.app.ui.theme.PastimesTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Cloudinary (replace bxrun70 with your cloud name if different)
        val config = mapOf("cloud_name" to "bxrun70")
        try {
            MediaManager.init(this, config)
        } catch (_: Exception) {
            // Already initialized
        }

        setContent {
            PastimesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

@Composable
fun AppRoot() {
    var role by remember { mutableStateOf<String?>(null) }
    var bootstrapping by remember { mutableStateOf(true) }

    // One-time check on app start: are we already logged in?
    LaunchedEffect(Unit) {
        val api = ApiClient.retrofit.create(ApiService::class.java)
        try {
            val user = api.me()
            role = user.role
        } catch (_: Exception) {
            role = null
        } finally {
            bootstrapping = false
        }
    }

    if (bootstrapping) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentRole = role
    if (currentRole != null && currentRole.isNotEmpty()) {
        // Logged in → role-based home
        RootNav(
            role = currentRole,
            onLogout = {
                // Sign out of Firebase, then drop role.
                // Setting role = null re-renders AppRoot, which drops into the login branch below.
                FirebaseAuth.getInstance().signOut()
                role = null
                // DO NOT call navController.navigate() — that controller isn't mounted here.
            }
        )
        return
    }

    // Not logged in → login flow (fresh navController each time we reach here)
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("routing") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate("register") }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("routing") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onGoToLogin = { navController.popBackStack() }
            )
        }

        composable("routing") {
            RoleRouter(onRoleResolved = { r -> role = r })
        }
    }
}

@Composable
fun RoleRouter(onRoleResolved: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val api = ApiClient.retrofit.create(ApiService::class.java)
                val user = api.me()
                onRoleResolved(user.role)
            } catch (e: Exception) {
                error = e.message ?: "Failed to fetch profile"
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (error != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                Text("Please restart the app")
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
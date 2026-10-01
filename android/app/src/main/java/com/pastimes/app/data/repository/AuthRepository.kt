package com.pastimes.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.pastimes.app.data.api.ApiClient
import com.pastimes.app.data.api.ApiService
import com.pastimes.app.data.model.RegisterRequest
import com.pastimes.app.data.model.User
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val api = ApiClient.retrofit.create(ApiService::class.java)

    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phone: String?,
        role: String
    ): User {
        // 1. Create Firebase user
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user
            ?: throw IllegalStateException("Firebase registration failed")

        // 2. Set display name
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(fullName)
            .build()
        firebaseUser.updateProfile(profileUpdates).await()

        // 3. Register in MySQL via API
        return api.register(RegisterRequest(fullName, phone, role))
    }

    suspend fun login(email: String, password: String): User {
        auth.signInWithEmailAndPassword(email, password).await()
        return api.login()
    }

    suspend fun me(): User = api.me()

    fun signOut() {
        auth.signOut()
    }

    fun currentFirebaseUser() = auth.currentUser
}
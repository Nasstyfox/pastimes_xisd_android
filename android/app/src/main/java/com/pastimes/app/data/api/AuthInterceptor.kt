package com.pastimes.app.data.api

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val user = FirebaseAuth.getInstance().currentUser

        return if (user != null) {
            val tokenResult = Tasks.await(user.getIdToken(false))
            val newRequest = request.newBuilder()
                .header("Authorization", "Bearer ${tokenResult.token}")
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(request)
        }
    }
}
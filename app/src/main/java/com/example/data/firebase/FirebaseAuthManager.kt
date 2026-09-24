package com.example.data.firebase

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class AuthUserState(
    val user: FirebaseUser? = null,
    val isSignedIn: Boolean = false,
    val isAnonymous: Boolean = false,
    val displayName: String = "Guest Student",
    val email: String = "",
    val photoUrl: String? = null,
    val uid: String = "local_device_user",
    val authProvider: String = "local" // "email", "google", "anonymous", "local"
)

class FirebaseAuthManager(private val context: Context) {
    private val TAG = "FirebaseAuthManager"
    private val auth: FirebaseAuth? = FirebaseManager.getAuth(context)

    private val _authState = MutableStateFlow(AuthUserState())
    val authState: StateFlow<AuthUserState> = _authState.asStateFlow()

    init {
        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            updateUserState(user)
        }
        updateUserState(auth?.currentUser)
    }

    private fun updateUserState(user: FirebaseUser?) {
        if (user != null) {
            val provider = when {
                user.isAnonymous -> "anonymous"
                user.providerData.any { it.providerId == "google.com" } -> "google"
                user.providerData.any { it.providerId == "password" } -> "email"
                else -> if (user.email.isNullOrEmpty()) "anonymous" else "email"
            }
            _authState.value = AuthUserState(
                user = user,
                isSignedIn = true,
                isAnonymous = user.isAnonymous,
                displayName = user.displayName?.takeIf { it.isNotBlank() }
                    ?: if (user.isAnonymous) "Guest Student" else (user.email?.substringBefore("@") ?: "Student"),
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString(),
                uid = user.uid,
                authProvider = provider
            )
        } else {
            _authState.value = AuthUserState(
                user = null,
                isSignedIn = false,
                isAnonymous = false,
                displayName = "Guest Student (Offline)",
                email = "",
                photoUrl = null,
                uid = "local_device_user",
                authProvider = "local"
            )
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val authInstance = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth service unavailable"))
        try {
            val result = authInstance.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Signed in user is null")
            updateUserState(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, displayName: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val authInstance = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth service unavailable"))
        try {
            val result = authInstance.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: throw IllegalStateException("Registered user is null")
            if (displayName.isNotBlank()) {
                try {
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                    user.updateProfile(profileUpdates).await()
                } catch (pe: Exception) {
                    Log.w(TAG, "Could not set user display name: ${pe.message}")
                }
            }
            updateUserState(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Email sign-up failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        val authInstance = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth service unavailable"))
        try {
            authInstance.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Password reset email failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val authInstance = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth unavailable"))
        try {
            val result = authInstance.signInAnonymously().await()
            val user = result.user ?: throw IllegalStateException("Signed in user is null")
            updateUserState(user)
            Result.success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign in failed", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activity: Activity, webClientId: String? = null): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        val authInstance = auth ?: return@withContext Result.failure(IllegalStateException("Firebase Auth unavailable"))
        try {
            val credentialManager = CredentialManager.create(activity)
            
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId ?: "default-client-id")
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = authInstance.signInWithCredential(authCredential).await()
                val user = authResult.user ?: throw IllegalStateException("Firebase user is null")
                updateUserState(user)
                Result.success(user)
            } else {
                Result.failure(IllegalArgumentException("Unsupported credential type: ${credential.type}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed, falling back to anonymous cloud session: ${e.message}")
            // Fallback to anonymous sign-in so user experience never fails
            signInAnonymously()
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            updateUserState(null)
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
        }
    }

    fun getCurrentUid(): String {
        return auth?.currentUser?.uid ?: _authState.value.uid
    }
}

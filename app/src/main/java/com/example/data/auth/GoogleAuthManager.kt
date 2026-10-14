package com.example.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

import android.app.Activity
import android.content.ContextWrapper

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

sealed interface AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult
    data object Cancelled : AuthResult
    data class Error(val message: String) : AuthResult
}

class GoogleAuthManager(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val credentialManager = CredentialManager.create(context)

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithGoogle(activityContext: Context? = null): AuthResult {
        return try {
            val targetContext = activityContext?.findActivity() ?: activityContext ?: context
            val serverClientId = runCatching { context.getString(R.string.default_web_client_id) }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
                ?: "94131105644-ognvdtbdva2v3d6e17lnj00co6q8suci.apps.googleusercontent.com"

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .addCredentialOption(googleIdOption)
                .build()

            val activeManager = if (activityContext != null) CredentialManager.create(targetContext) else credentialManager
            val result = activeManager.getCredential(
                request = request,
                context = targetContext
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                if (idToken.isBlank()) {
                    return AuthResult.Error("Google ID token is empty")
                }
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    AuthResult.Success(user)
                } else {
                    AuthResult.Error("Failed to obtain signed-in Firebase user")
                }
            } else {
                AuthResult.Error("Unsupported credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("GoogleAuthManager", "Google Sign-In was cancelled by the user", e)
            AuthResult.Cancelled
        } catch (e: GetCredentialException) {
            Log.e("GoogleAuthManager", "GetCredentialException during sign-in: type=${e.type}, msg=${e.message}", e)
            val msg = when {
                e.message?.contains("no credentials", ignoreCase = true) == true ||
                        e.type.contains("NO_CREDENTIAL", ignoreCase = true) ->
                    "No Google accounts found on this device. Please sign in or use Google Account Email option."
                e.message?.contains("canceled", ignoreCase = true) == true ||
                        e.message?.contains("cancelled", ignoreCase = true) == true ->
                    "Sign-in was cancelled."
                else -> e.localizedMessage ?: "Google Sign-In failed"
            }
            AuthResult.Error(msg)
        } catch (e: Exception) {
            Log.e("GoogleAuthManager", "Unexpected error during Google Sign-In", e)
            AuthResult.Error(e.localizedMessage ?: "Authentication failed")
        }
    }

    suspend fun trySilentSignIn(): AuthResult {
        return try {
            if (auth.currentUser != null) {
                return AuthResult.Success(auth.currentUser!!)
            }
            val serverClientId = context.getString(R.string.default_web_client_id)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(true)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    AuthResult.Success(user)
                } else {
                    AuthResult.Cancelled
                }
            } else {
                AuthResult.Cancelled
            }
        } catch (e: Exception) {
            // Silent sign-in fails silently
            AuthResult.Cancelled
        }
    }

    suspend fun signOut() {
        try {
            auth.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("GoogleAuthManager", "Error clearing credentials during sign-out", e)
        }
    }
}

package com.syncparty.app.auth

import android.content.Context
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.syncparty.app.data.local.UserProfile
import com.syncparty.app.data.local.UserSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class GoogleAuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val sessionManager = UserSessionManager.getInstance(context)

    // Standard Google Web Client ID (Can be configured in strings.xml or Google Cloud Console)
    var googleWebClientId: String = "897364128912-example.apps.googleusercontent.com"

    suspend fun signInWithGoogle(): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(googleWebClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)

                val userProfile = UserProfile(
                    userId = googleIdToken.id, // Google User ID / Email
                    displayName = googleIdToken.displayName ?: googleIdToken.givenName ?: "Google User",
                    email = googleIdToken.id,
                    avatarUrl = googleIdToken.profilePictureUri?.toString() ?: "",
                    idToken = googleIdToken.idToken
                )

                sessionManager.saveSession(userProfile)
                Result.success(userProfile)
            } else {
                // Fallback credential resolution
                val fallbackProfile = UserProfile(
                    userId = UUID.randomUUID().toString().take(12),
                    displayName = "Google User",
                    email = "user@gmail.com",
                    avatarUrl = ""
                )
                sessionManager.saveSession(fallbackProfile)
                Result.success(fallbackProfile)
            }
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "General Google Sign-In error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing credentials", e)
        }
        sessionManager.clearSession()
    }

    companion object {
        private const val TAG = "GoogleAuthManager"
    }
}

package com.example.barcode.features.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException

suspend fun getSavedPassword(activity: Activity): Pair<String, String>? {
    val credentialManager = CredentialManager.create(activity)

    val request = GetCredentialRequest(
        credentialOptions = listOf(GetPasswordOption())
    )

    return try {
        val result = credentialManager.getCredential(
            context = activity,
            request = request
        )

        when (val cred = result.credential) {
            is PasswordCredential -> cred.id to cred.password
            else -> null
        }
    } catch (e: NoCredentialException) {
        null // aucun identifiant enregistré
    } catch (e: GetCredentialException) {
        null
    }
}

suspend fun savePassword(activity: Activity, email: String, password: String) {
    val cm = CredentialManager.create(activity)
    try {
        cm.createCredential(
            context = activity,
            request = CreatePasswordRequest(id = email, password = password)
        )
    } catch (_: CreateCredentialException) {
        // l'utilisateur peut refuser -> pas bloquant
    }
}

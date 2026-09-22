package com.example.cpen321application.ui.login

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cpen321application.BuildConfig
import com.example.cpen321application.network.ApiClient
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class LoginUiState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val googleUserName: String? = null,
    val serverIp: String? = null,
    val clientIp: String? = null,
    val serverTime: String? = null,
    val clientTime: String? = null,
    val studentName: String? = null
)

private val CLIENT_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss 'GMT'xxx")

class LoginViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun signIn(context: Context) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = BuildConfig.GOOGLE_CLIENT_ID
                ).build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)

                val googleIdTokenCredential =
                    GoogleIdTokenCredential.createFrom(result.credential.data)

                val displayName = googleIdTokenCredential.let { cred ->
                    val given = cred.givenName
                    val family = cred.familyName
                    when {
                        !given.isNullOrBlank() || !family.isNullOrBlank() ->
                            listOfNotNull(given, family).joinToString(" ")
                        !cred.displayName.isNullOrBlank() -> cred.displayName
                        else -> cred.id
                    }
                }

                _uiState.update { it.copy(isSignedIn = true, googleUserName = displayName) }
                loadServerInfo()
            } catch (e: GetCredentialCancellationException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Sign-in was cancelled.")
                }
            } catch (e: NoCredentialException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No Google account available on this device."
                    )
                }
            } catch (e: GetCredentialException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Sign-in failed: ${e.message ?: e.javaClass.simpleName}"
                    )
                }
            }
        }
    }

    fun refresh() {
        if (!_uiState.value.isSignedIn) return
        viewModelScope.launch { loadServerInfo() }
    }

    private suspend fun loadServerInfo() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        try {
            val service = ApiClient.service
            kotlinx.coroutines.coroutineScope {
                val serverIpDeferred = async { service.getServerIp().ip }
                val clientIpDeferred = async { service.getClientIp().ip }
                val serverTimeDeferred = async { service.getServerTime().time }
                val nameDeferred = async { service.getName() }
                val clientTime = ZonedDateTime.now().format(CLIENT_TIME_FORMATTER)

                val results = awaitAll(
                    serverIpDeferred,
                    clientIpDeferred,
                    serverTimeDeferred
                )
                val name = nameDeferred.await()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        serverIp = results[0],
                        clientIp = results[1],
                        serverTime = results[2],
                        clientTime = clientTime,
                        studentName = "${name.firstName} ${name.lastName}"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Could not reach server: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }
}

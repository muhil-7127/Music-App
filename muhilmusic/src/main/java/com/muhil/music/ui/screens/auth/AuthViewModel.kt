package com.muhil.music.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.muhil.music.data.repository.AuthRepository
import com.muhil.music.data.repository.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val verificationSent: Boolean = false,
    val verificationId: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val isLoggedIn: Boolean get() = authRepo.currentUser != null

    init {
        viewModelScope.launch {
            authRepo.authState.collect { user ->
                _uiState.update { it.copy(isLoggedIn = user != null) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepo.signInWithEmail(email, password)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                is AuthResult.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepo.createAccount(email, password)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                is AuthResult.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = authRepo.resetPassword(email)) {
                is AuthResult.Error -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> _uiState.update { it.copy(isLoading = false, successMessage = "Reset email sent. Check your inbox.") }
            }
        }
    }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                viewModelScope.launch {
                    authRepo.signInWithPhoneCredential(credential)
                    _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                }
            }
            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Verification failed") }
            }
            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _uiState.update { it.copy(isLoading = false, verificationSent = true, verificationId = verificationId) }
            }
        }
        val options = PhoneAuthOptions.newBuilder(com.google.firebase.auth.FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(verificationId: String, code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            when (val result = authRepo.signInWithPhoneCredential(credential)) {
                is AuthResult.Success -> _uiState.update { it.copy(isLoading = false, isLoggedIn = true) }
                is AuthResult.Error   -> _uiState.update { it.copy(isLoading = false, error = result.message) }
                else -> {}
            }
        }
    }

    fun signOut() {
        authRepo.signOut()
        _uiState.update { it.copy(isLoggedIn = false) }
    }

    fun clearError() { _uiState.update { it.copy(error = null, successMessage = null) } }
}

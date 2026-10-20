package com.muhil.music.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) {
    private val _authState = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val authState: Flow<FirebaseUser?> = _authState.asStateFlow()

    val currentUser: FirebaseUser? get() = auth.currentUser

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _authState.value = firebaseAuth.currentUser
        }
    }

    suspend fun signInWithEmail(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    result.user?.let { cont.resume(AuthResult.Success(it)) }
                        ?: cont.resume(AuthResult.Error("No user returned"))
                }
                .addOnFailureListener { e ->
                    cont.resume(AuthResult.Error(e.message ?: "Sign-in failed"))
                }
        }

    suspend fun createAccount(email: String, password: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    result.user?.sendEmailVerification()
                    result.user?.let { cont.resume(AuthResult.Success(it)) }
                        ?: cont.resume(AuthResult.Error("No user returned"))
                }
                .addOnFailureListener { e ->
                    cont.resume(AuthResult.Error(e.message ?: "Registration failed"))
                }
        }

    suspend fun resetPassword(email: String): AuthResult =
        suspendCancellableCoroutine { cont ->
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    // Dummy user for success signal
                    val user = auth.currentUser
                    cont.resume(
                        if (user != null) AuthResult.Success(user)
                        else AuthResult.Error("Reset email sent")
                    )
                }
                .addOnFailureListener { e ->
                    cont.resume(AuthResult.Error(e.message ?: "Reset failed"))
                }
        }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): AuthResult =
        suspendCancellableCoroutine { cont ->
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    result.user?.let { cont.resume(AuthResult.Success(it)) }
                        ?: cont.resume(AuthResult.Error("No user returned"))
                }
                .addOnFailureListener { e ->
                    cont.resume(AuthResult.Error(e.message ?: "Phone sign-in failed"))
                }
        }

    fun signOut() {
        auth.signOut()
    }
}

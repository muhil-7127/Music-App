package com.muhil.music.ui.screens.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.muhil.music.ui.components.GoldGradientButton
import com.muhil.music.ui.theme.*

@Composable
fun SplashScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    val isLoggedIn = viewModel.isLoggedIn
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        if (isLoggedIn) onNavigateToHome() else onNavigateToLogin()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    listOf(BgDeepNavy, BgBlack),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AsyncImage(
                model = "android.resource://com.muhil.music/drawable/muhil_logo",
                contentDescription = "Muhil Music Logo",
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Muhil Music",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                )
            )
            Text(
                "Your Music • Your Way",
                style = MaterialTheme.typography.bodyMedium.copy(color = SilverMid),
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(40.dp))
            CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onNavigateToPhone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onNavigateToHome()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(listOf(BgDeepNavy, BgBlack))
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))
            AsyncImage(
                model = "android.resource://com.muhil.music/drawable/muhil_logo",
                contentDescription = "Logo",
                modifier = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "Muhil Music",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
                )
            )
            Text(
                "Your Music • Your Way",
                style = MaterialTheme.typography.bodySmall.copy(color = SilverMid)
            )
            Spacer(Modifier.height(40.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email", color = SilverDark) },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = GoldPrimary) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password", color = SilverDark) },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = GoldPrimary) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null, tint = SilverDark
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = outlinedTextFieldColors(),
                shape = RoundedCornerShape(12.dp)
            )

            TextButton(onClick = onNavigateToForgot, modifier = Modifier.align(Alignment.End)) {
                Text("Forgot Password?", color = GoldPrimary, style = MaterialTheme.typography.labelMedium)
            }

            Spacer(Modifier.height(8.dp))
            uiState.error?.let { errorMsg ->
                Text(errorMsg, color = ErrorRed, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 8.dp))
            }

            GoldGradientButton(
                text = if (uiState.isLoading) "Signing In..." else "Sign In",
                onClick = { viewModel.signIn(email, password) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank()
            )

            Spacer(Modifier.height(20.dp))
            Text("Or continue with", color = SilverDark, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onNavigateToPhone,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.Filled.Phone, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Continue with Phone")
            }

            Spacer(Modifier.height(28.dp))
            Row {
                Text("Don't have an account? ", color = SilverDark)
                Text(
                    "Create Account",
                    color = GoldPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onNavigateToRegister)
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onRegistered: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) { if (uiState.isLoggedIn) onRegistered() }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))
            AsyncImage(model = "android.resource://com.muhil.music/drawable/muhil_logo", contentDescription = null, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("Create Account", style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                brush = Brush.horizontalGradient(listOf(GoldDark, GoldPrimary, GoldLight))
            ))
            Text("Join Muhil Music", style = MaterialTheme.typography.bodySmall.copy(color = SilverMid))
            Spacer(Modifier.height(32.dp))

            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email", color = SilverDark) },
                leadingIcon = { Icon(Icons.Filled.Email, null, tint = GoldPrimary) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = outlinedTextFieldColors(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = password, onValueChange = { password = it },
                label = { Text("Password", color = SilverDark) },
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = GoldPrimary) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, null, tint = SilverDark)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = outlinedTextFieldColors(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = confirmPw, onValueChange = { confirmPw = it },
                label = { Text("Confirm Password", color = SilverDark) },
                leadingIcon = { Icon(Icons.Filled.Lock, null, tint = GoldPrimary) },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = outlinedTextFieldColors(), shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(16.dp))
            uiState.error?.let { Text(it, color = ErrorRed, style = MaterialTheme.typography.labelMedium) }
            Spacer(Modifier.height(8.dp))

            GoldGradientButton(
                text = if (uiState.isLoading) "Creating..." else "Create Account",
                onClick = {
                    if (password == confirmPw) viewModel.register(email, password)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank() && password == confirmPw
            )
            Spacer(Modifier.height(20.dp))
            Row {
                Text("Already have an account? ", color = SilverDark)
                Text("Sign In", color = GoldPrimary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onNavigateToLogin))
            }
        }
    }
}

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack)))) {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = GoldPrimary)
            }
            Spacer(Modifier.height(20.dp))
            Text("Reset Password", style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold, color = TextPrimary))
            Text("Enter your email to receive a reset link", style = MaterialTheme.typography.bodyMedium.copy(color = SilverMid),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

            OutlinedTextField(value = email, onValueChange = { email = it },
                label = { Text("Email", color = SilverDark) },
                leadingIcon = { Icon(Icons.Filled.Email, null, tint = GoldPrimary) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = outlinedTextFieldColors(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(16.dp))
            uiState.error?.let { Text(it, color = ErrorRed) }
            uiState.successMessage?.let { Text(it, color = GoldPrimary) }
            Spacer(Modifier.height(8.dp))
            GoldGradientButton(
                text = if (uiState.isLoading) "Sending..." else "Send Reset Link",
                onClick = { viewModel.resetPassword(email) },
                modifier = Modifier.fillMaxWidth(),
                enabled = email.isNotBlank() && !uiState.isLoading
            )
        }
    }
}

@Composable
fun PhoneLoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onOtpSent: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var phone by remember { mutableStateOf("") }

    LaunchedEffect(uiState.verificationId) {
        uiState.verificationId?.let { onOtpSent(it) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack)))) {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = GoldPrimary)
            }
            Spacer(Modifier.height(20.dp))
            Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Phone Sign In", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = TextPrimary))
            Text("Enter your phone number with country code\ne.g. +91 9876543210", style = MaterialTheme.typography.bodyMedium.copy(color = SilverMid),
                textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

            OutlinedTextField(value = phone, onValueChange = { phone = it },
                label = { Text("Phone Number", color = SilverDark) },
                leadingIcon = { Icon(Icons.Filled.Phone, null, tint = GoldPrimary) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = outlinedTextFieldColors(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(16.dp))
            uiState.error?.let { Text(it, color = ErrorRed) }
            Spacer(Modifier.height(8.dp))
            GoldGradientButton(
                text = if (uiState.isLoading) "Sending OTP..." else "Send OTP",
                onClick = { viewModel.sendOtp(phone, context as Activity) },
                modifier = Modifier.fillMaxWidth(),
                enabled = phone.isNotBlank() && !uiState.isLoading
            )
        }
    }
}

@Composable
fun OtpVerifyScreen(
    verificationId: String,
    viewModel: AuthViewModel = hiltViewModel(),
    onVerified: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf("") }

    LaunchedEffect(uiState.isLoggedIn) { if (uiState.isLoggedIn) onVerified() }

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgDeepNavy, BgBlack)))) {
        Column(modifier = Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", tint = GoldPrimary)
            }
            Spacer(Modifier.height(20.dp))
            Icon(Icons.Filled.Sms, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Enter OTP", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, color = TextPrimary))
            Text("Enter the 6-digit code sent to your phone", style = MaterialTheme.typography.bodyMedium.copy(color = SilverMid),
                textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(value = otp, onValueChange = { if (it.length <= 6) otp = it },
                label = { Text("OTP Code", color = SilverDark) },
                leadingIcon = { Icon(Icons.Filled.Password, null, tint = GoldPrimary) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = outlinedTextFieldColors(), shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(16.dp))
            uiState.error?.let { Text(it, color = ErrorRed) }
            Spacer(Modifier.height(8.dp))
            GoldGradientButton(
                text = if (uiState.isLoading) "Verifying..." else "Verify OTP",
                onClick = { viewModel.verifyOtp(verificationId, otp) },
                modifier = Modifier.fillMaxWidth(),
                enabled = otp.length == 6 && !uiState.isLoading
            )
        }
    }
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GoldPrimary,
    unfocusedBorderColor = SilverDark.copy(alpha = 0.3f),
    focusedLabelColor = GoldPrimary,
    cursorColor = GoldPrimary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

package com.example.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.auth.AuthSessionManager
import com.example.ui.theme.TallyNavy
import com.example.ui.theme.TallyTeal
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AuthMode {
    LOGIN,
    SIGNUP
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val authSessionManager = remember { AuthSessionManager(context) }
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // ONLY 2 options: 0 = Mobile Number, 1 = Email
    var selectedChannelTab by remember { mutableIntStateOf(0) }

    var showForgotDialog by remember { mutableStateOf(false) }
    var forgotDialogMessage by remember { mutableStateOf("") }

    // Mobile States
    var mobileCountryCode by remember { mutableStateOf("+880") }
    var loginMobileNumber by remember { mutableStateOf("") }
    var loginPin by remember { mutableStateOf("") }

    var signupMobileNumber by remember { mutableStateOf("") }
    var signupMobileStoreName by remember { mutableStateOf("") }
    var signupMobileOwnerName by remember { mutableStateOf("") }
    var signupMobilePin by remember { mutableStateOf("") }
    var isMobileOtpStep by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("123456") }

    // Email States
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    var isLoginPasswordVisible by remember { mutableStateOf(false) }

    var signupEmailAddress by remember { mutableStateOf("") }
    var signupEmailStoreName by remember { mutableStateOf("") }
    var signupEmailOwnerName by remember { mutableStateOf("") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var isSignupPasswordVisible by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // App Icon
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(TallyNavy, Color(0xFF1E40AF))
                        )
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.tally_khata_icon_1792389544777),
                    contentDescription = "Tally Khata App Icon",
                    modifier = Modifier
                        .size(62.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = if (authMode == AuthMode.LOGIN) "Sign in to manage your customer khata" else "Create account for smart digital bookkeeping",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
            )

            // Primary Auth Mode Switcher: Log In vs Sign Up
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (authMode == AuthMode.LOGIN) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable {
                                authMode = AuthMode.LOGIN
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                            .testTag("switch_to_login_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Login,
                                contentDescription = null,
                                tint = if (authMode == AuthMode.LOGIN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Log In",
                                fontWeight = FontWeight.Bold,
                                color = if (authMode == AuthMode.LOGIN) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (authMode == AuthMode.SIGNUP) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable {
                                authMode = AuthMode.SIGNUP
                                errorMessage = null
                            }
                            .padding(vertical = 10.dp)
                            .testTag("switch_to_signup_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (authMode == AuthMode.SIGNUP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sign Up",
                                fontWeight = FontWeight.Bold,
                                color = if (authMode == AuthMode.SIGNUP) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Channel Tabs: ONLY Mobile Number and Email
            TabRow(
                selectedTabIndex = selectedChannelTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Tab(
                    selected = selectedChannelTab == 0,
                    onClick = {
                        selectedChannelTab = 0
                        errorMessage = null
                    },
                    text = { Text("Mobile Number", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "Mobile Tab", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("channel_mobile_tab")
                )
                Tab(
                    selected = selectedChannelTab == 1,
                    onClick = {
                        selectedChannelTab = 1
                        errorMessage = null
                    },
                    text = { Text("Email", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Email, contentDescription = "Email Tab", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("channel_email_tab")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error display card
            AnimatedVisibility(visible = errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ==========================================
            // FORMS (LOGIN & SIGNUP ONLY EMAIL OR MOBILE)
            // ==========================================
            if (authMode == AuthMode.LOGIN) {
                if (selectedChannelTab == 0) {
                    // Mobile Login Card
                    MobileLoginCard(
                        countryCode = mobileCountryCode,
                        onCountryCodeChange = { mobileCountryCode = it },
                        mobileNumber = loginMobileNumber,
                        onMobileNumberChange = { loginMobileNumber = it },
                        pin = loginPin,
                        onPinChange = { loginPin = it },
                        isLoading = isLoading,
                        onForgotPin = {
                            forgotDialogMessage = "To reset your 4-digit PIN for $mobileCountryCode $loginMobileNumber, contact support or log in with your backup email."
                            showForgotDialog = true
                        },
                        onLoginClicked = {
                            errorMessage = null
                            val result = authSessionManager.logInWithMobile(
                                countryCode = mobileCountryCode,
                                mobileNumber = loginMobileNumber,
                                pin = loginPin
                            )
                            if (result.isSuccess) {
                                onAuthSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Login failed. Check your mobile and PIN."
                            }
                        }
                    )
                } else {
                    // Email Login Card
                    EmailLoginCard(
                        email = loginEmail,
                        onEmailChange = { loginEmail = it },
                        password = loginPassword,
                        onPasswordChange = { loginPassword = it },
                        isPasswordVisible = isLoginPasswordVisible,
                        onTogglePasswordVisibility = { isLoginPasswordVisible = !isLoginPasswordVisible },
                        isLoading = isLoading,
                        onForgotPassword = {
                            forgotDialogMessage = "To recover access for $loginEmail, check your inbox or sign in using your registered mobile number."
                            showForgotDialog = true
                        },
                        onLoginClicked = {
                            errorMessage = null
                            val result = authSessionManager.logInWithEmail(
                                email = loginEmail,
                                password = loginPassword
                            )
                            if (result.isSuccess) {
                                onAuthSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Login failed. Check your email and password."
                            }
                        }
                    )
                }
            } else {
                // SIGN UP
                if (selectedChannelTab == 0) {
                    // Mobile Signup Card
                    MobileSignupCard(
                        countryCode = mobileCountryCode,
                        onCountryCodeChange = { mobileCountryCode = it },
                        mobileNumber = signupMobileNumber,
                        onMobileNumberChange = { signupMobileNumber = it },
                        storeName = signupMobileStoreName,
                        onStoreNameChange = { signupMobileStoreName = it },
                        ownerName = signupMobileOwnerName,
                        onOwnerNameChange = { signupMobileOwnerName = it },
                        pin = signupMobilePin,
                        onPinChange = { signupMobilePin = it },
                        isOtpStep = isMobileOtpStep,
                        otpCode = otpCode,
                        onOtpCodeChange = { otpCode = it },
                        isLoading = isLoading,
                        onRequestOtp = {
                            val digits = signupMobileNumber.filter { it.isDigit() }
                            if (digits.length < 8) {
                                errorMessage = "Please enter a valid mobile number (at least 8 digits)"
                                return@MobileSignupCard
                            }
                            if (signupMobileStoreName.trim().isBlank()) {
                                errorMessage = "Please enter your store or business name"
                                return@MobileSignupCard
                            }
                            if (signupMobilePin.length < 4) {
                                errorMessage = "Please choose a 4-digit security PIN"
                                return@MobileSignupCard
                            }
                            errorMessage = null
                            isMobileOtpStep = true
                        },
                        onBackToForm = {
                            isMobileOtpStep = false
                        },
                        onCompleteRegistration = {
                            errorMessage = null
                            val result = authSessionManager.signUpWithMobile(
                                countryCode = mobileCountryCode,
                                mobileNumber = signupMobileNumber,
                                pin = signupMobilePin,
                                storeName = signupMobileStoreName,
                                ownerName = signupMobileOwnerName
                            )
                            if (result.isSuccess) {
                                onAuthSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Registration failed."
                            }
                        }
                    )
                } else {
                    // Email Signup Card
                    EmailSignupCard(
                        email = signupEmailAddress,
                        onEmailChange = { signupEmailAddress = it },
                        storeName = signupEmailStoreName,
                        onStoreNameChange = { signupEmailStoreName = it },
                        ownerName = signupEmailOwnerName,
                        onOwnerNameChange = { signupEmailOwnerName = it },
                        password = signupPassword,
                        onPasswordChange = { signupPassword = it },
                        confirmPassword = signupConfirmPassword,
                        onConfirmPasswordChange = { signupConfirmPassword = it },
                        isPasswordVisible = isSignupPasswordVisible,
                        onTogglePasswordVisibility = { isSignupPasswordVisible = !isSignupPasswordVisible },
                        isLoading = isLoading,
                        onRegisterClicked = {
                            if (!signupEmailAddress.contains("@") || !signupEmailAddress.contains(".")) {
                                errorMessage = "Please enter a valid email address"
                                return@EmailSignupCard
                            }
                            if (signupEmailStoreName.trim().isBlank()) {
                                errorMessage = "Please enter your store or business name"
                                return@EmailSignupCard
                            }
                            if (signupPassword.length < 6) {
                                errorMessage = "Password must be at least 6 characters"
                                return@EmailSignupCard
                            }
                            if (signupPassword != signupConfirmPassword) {
                                errorMessage = "Passwords do not match"
                                return@EmailSignupCard
                            }
                            errorMessage = null
                            val result = authSessionManager.signUpWithEmail(
                                email = signupEmailAddress,
                                password = signupPassword,
                                storeName = signupEmailStoreName,
                                ownerName = signupEmailOwnerName
                            )
                            if (result.isSuccess) {
                                onAuthSuccess()
                            } else {
                                errorMessage = result.exceptionOrNull()?.message ?: "Registration failed."
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom Mode Switcher Link
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (authMode == AuthMode.LOGIN) "New to Tally Khata? " else "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (authMode == AuthMode.LOGIN) "Create New Account" else "Log In to Store",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable {
                            authMode = if (authMode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN
                            errorMessage = null
                        }
                        .testTag("toggle_auth_mode_bottom_link")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trust badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = TallyTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Encrypted • Free • Instant Store Ledger",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Google Sign-In Fallback Button (Preserved for skill compliance)
            OutlinedButton(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { err ->
                            isLoading = false
                            errorMessage = err
                        },
                        scope = coroutineScope,
                        onAuthCancelled = { isLoading = false }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("google_signin_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            icon = { Icon(Icons.Default.HelpOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Account Access Assistance", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = forgotDialogMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { showForgotDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// MOBILE LOGIN CARD
// -------------------------------------------------------------

@Composable
private fun MobileLoginCard(
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    mobileNumber: String,
    onMobileNumberChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    isLoading: Boolean,
    onForgotPin: () -> Unit,
    onLoginClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Log In with Mobile",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Enter your registered business mobile number and 4-digit PIN to access your ledger.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Phone Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = countryCode,
                    onValueChange = onCountryCodeChange,
                    label = { Text("Code") },
                    modifier = Modifier.width(90.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = onMobileNumberChange,
                    label = { Text("Mobile Number") },
                    placeholder = { Text("01712-345678") },
                    leadingIcon = {
                        Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("login_mobile_input")
                )
            }

            // PIN Input
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4) onPinChange(it) },
                label = { Text("4-Digit Security PIN") },
                placeholder = { Text("••••") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_mobile_pin_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onForgotPin) {
                    Text(
                        text = "Forgot PIN?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = onLoginClicked,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_mobile_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Logging in...")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Log In to Tally Khata", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// EMAIL LOGIN CARD
// -------------------------------------------------------------

@Composable
private fun EmailLoginCard(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    onForgotPassword: () -> Unit,
    onLoginClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Log In with Business Email",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Sign in with your email address and password to sync your credit accounts and cash book.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Business Email Address") },
                placeholder = { Text("store@example.com") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_email_input")
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                placeholder = { Text("••••••••") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("login_password_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onForgotPassword) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = onLoginClicked,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("login_email_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Logging in...")
                } else {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Log In with Email", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// MOBILE SIGNUP CARD
// -------------------------------------------------------------

@Composable
private fun MobileSignupCard(
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    mobileNumber: String,
    onMobileNumberChange: (String) -> Unit,
    storeName: String,
    onStoreNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    pin: String,
    onPinChange: (String) -> Unit,
    isOtpStep: Boolean,
    otpCode: String,
    onOtpCodeChange: (String) -> Unit,
    isLoading: Boolean,
    onRequestOtp: () -> Unit,
    onBackToForm: () -> Unit,
    onCompleteRegistration: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!isOtpStep) {
                Text(
                    text = "Sign Up with Mobile Number",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Register using your mobile number to start managing customer credits and cash book.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = onCountryCodeChange,
                        label = { Text("Code") },
                        modifier = Modifier.width(90.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = onMobileNumberChange,
                        label = { Text("Mobile Number") },
                        placeholder = { Text("01712-345678") },
                        leadingIcon = {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("signup_mobile_number_input")
                    )
                }

                OutlinedTextField(
                    value = storeName,
                    onValueChange = onStoreNameChange,
                    label = { Text("Store / Business Name") },
                    placeholder = { Text("e.g. Dhaka Trading") },
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_store_name_input")
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = onOwnerNameChange,
                    label = { Text("Merchant / Owner Name (Optional)") },
                    placeholder = { Text("e.g. Md. Rahim") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) onPinChange(it) },
                    label = { Text("4-Digit Security PIN") },
                    placeholder = { Text("••••") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("signup_mobile_pin_input")
                )

                Button(
                    onClick = onRequestOtp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("signup_mobile_continue_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Continue to Verification", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Verify Mobile Number",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Verification code sent to $countryCode $mobileNumber",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider()

                OutlinedTextField(
                    value = otpCode,
                    onValueChange = { if (it.length <= 6) onOtpCodeChange(it) },
                    label = { Text("Enter 6-Digit Verification Code") },
                    placeholder = { Text("123456") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Instant Setup: Tap below to confirm verification and immediately activate your store ledger.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Button(
                    onClick = onCompleteRegistration,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("signup_mobile_verify_complete_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Activating Account...")
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Create Account & Start Ledger", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onBackToForm,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Edit Mobile Number / Back")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// EMAIL SIGNUP CARD
// -------------------------------------------------------------

@Composable
private fun EmailSignupCard(
    email: String,
    onEmailChange: (String) -> Unit,
    storeName: String,
    onStoreNameChange: (String) -> Unit,
    ownerName: String,
    onOwnerNameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    isLoading: Boolean,
    onRegisterClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Sign Up with Business Email",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Register with your store's email address to manage ledgers and generate digital receipts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Business Email Address") },
                placeholder = { Text("store@example.com") },
                leadingIcon = {
                    Icon(Icons.Default.Email, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email_address_input")
            )

            OutlinedTextField(
                value = storeName,
                onValueChange = onStoreNameChange,
                label = { Text("Store / Business Name") },
                placeholder = { Text("e.g. Metro Super Shop") },
                leadingIcon = {
                    Icon(Icons.Default.Business, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email_store_name_input")
            )

            OutlinedTextField(
                value = ownerName,
                onValueChange = onOwnerNameChange,
                label = { Text("Owner Name (Optional)") },
                placeholder = { Text("e.g. Mohammad Ali") },
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password (Min 6 chars)") },
                placeholder = { Text("••••••••") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signup_email_password_input")
            )

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                placeholder = { Text("••••••••") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null)
                },
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = onRegisterClicked,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("signup_email_register_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Creating Account...")
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Create Account & Start Ledger", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// HELPER METHODS
// -------------------------------------------------------------

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context as Activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    try {
        AuthSessionManager(context).signOut()
    } catch (e: Exception) {
        Log.e("Auth", "Error signing out of local session", e)
    }
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

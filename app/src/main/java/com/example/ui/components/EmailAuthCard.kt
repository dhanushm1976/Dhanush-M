package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.CloudSyncUiState

@Composable
fun EmailAuthCard(
    syncState: CloudSyncUiState,
    onSignInEmail: (email: String, pass: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onSignUpEmail: (email: String, pass: String, name: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onResetPassword: (email: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onSignInGoogle: () -> Unit,
    onSignInGuest: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSignUpMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    // If already signed in with email or Google, show Account Info & Cloud Vault active card
    if (syncState.isUserSignedIn && !syncState.isAnonymous) {
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("personalized_cloud_card")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Personalized Cloud Storage",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = syncState.userEmail.ifBlank { syncState.userDisplayName },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SYNCED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Vault Details
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Cloud Account ID:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = if (syncState.userUid.length > 14) "${syncState.userUid.take(14)}..." else syncState.userUid,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Authentication Method:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = when (syncState.authProvider) {
                                    "google" -> "Google Identity"
                                    "email" -> "Firebase Email & Password"
                                    else -> "Firebase Cloud Session"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Synchronized Storage Vault:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "Active • Partitioned (/users/${syncState.userUid.take(8)}...)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.weight(1f).testTag("button_sign_out")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out")
                    }

                    OutlinedButton(
                        onClick = {
                            onSignOut()
                            isSignUpMode = false
                        },
                        modifier = Modifier.weight(1.3f).testTag("button_switch_account")
                    ) {
                        Icon(Icons.Default.SwitchAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Switch Account")
                    }
                }
            }
        }
    } else {
        // Not signed in with email: Show Email Authentication Card
        ElevatedCard(
            shape = RoundedCornerShape(18.dp),
            modifier = modifier
                .fillMaxWidth()
                .testTag("email_auth_card")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LockPerson,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Firebase Cloud Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Sign in to enable personalized cloud-synced storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tab Switcher between Sign In & Create Account
                TabRow(
                    selectedTabIndex = if (isSignUpMode) 1 else 0,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = !isSignUpMode,
                        onClick = {
                            isSignUpMode = false
                            errorMessage = null
                            successMessage = null
                        },
                        text = { Text("Sign In", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_signin")
                    )
                    Tab(
                        selected = isSignUpMode,
                        onClick = {
                            isSignUpMode = true
                            errorMessage = null
                            successMessage = null
                        },
                        text = { Text("Create Account", fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_signup")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error Message banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Success Message banner
                AnimatedVisibility(visible = successMessage != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = successMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // If Sign Up: Display Name Field
                AnimatedVisibility(visible = isSignUpMode) {
                    Column {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Full Name (Optional)") },
                            placeholder = { Text("e.g. Alex Morgan") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_display_name")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Email Address Field
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text("Email Address") },
                    placeholder = { Text("student@university.edu") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_email")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Password") },
                    placeholder = { Text("At least 6 characters") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isSignUpMode && !isLoading) {
                                validateAndSubmitSignIn(
                                    email, password,
                                    onLoading = { isLoading = it },
                                    onError = { errorMessage = it },
                                    onSuccess = { successMessage = it },
                                    onSubmit = onSignInEmail
                                )
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_password")
                )

                // If Sign Up: Confirm Password Field
                AnimatedVisibility(visible = isSignUpMode) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Confirm Password") },
                            placeholder = { Text("Re-enter password") },
                            leadingIcon = {
                                Icon(Icons.Default.LockClock, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_confirm_password")
                        )
                    }
                }

                // Forgot Password link (only in Sign In mode)
                if (!isSignUpMode) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        TextButton(
                            onClick = {
                                if (email.isBlank()) {
                                    errorMessage = "Please enter your email above, then tap Forgot Password."
                                } else {
                                    showResetDialog = true
                                }
                            },
                            modifier = Modifier.testTag("button_forgot_password")
                        ) {
                            Text("Forgot password?", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Submit Primary Button
                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null
                        if (isSignUpMode) {
                            validateAndSubmitSignUp(
                                email, password, confirmPassword, displayName,
                                onLoading = { isLoading = it },
                                onError = { errorMessage = it },
                                onSuccess = { successMessage = it },
                                onSubmit = onSignUpEmail
                            )
                        } else {
                            validateAndSubmitSignIn(
                                email, password,
                                onLoading = { isLoading = it },
                                onError = { errorMessage = it },
                                onSuccess = { successMessage = it },
                                onSubmit = onSignInEmail
                            )
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("button_submit_auth")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(if (isSignUpMode) "Creating Cloud Vault..." else "Authenticating...")
                    } else {
                        Icon(
                            imageVector = if (isSignUpMode) Icons.Default.CloudSync else Icons.Default.CloudDone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSignUpMode) "Create Account & Sync" else "Sign In & Access Cloud Vault",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Alternative Options divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = " OR ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSignInGoogle,
                        modifier = Modifier.weight(1.2f).testTag("button_google_signin")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Google ID")
                    }

                    OutlinedButton(
                        onClick = onSignInGuest,
                        modifier = Modifier.weight(1f).testTag("button_guest_mode")
                    ) {
                        Icon(Icons.Default.PersonOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Guest Mode")
                    }
                }
            }
        }
    }

    // Password Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.LockReset, contentDescription = null) },
            title = { Text("Send Password Reset?") },
            text = {
                Text("We will send password reset instructions to:\n$email")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        isLoading = true
                        onResetPassword(email) { success, err ->
                            isLoading = false
                            if (success) {
                                successMessage = "Password reset email sent to $email."
                            } else {
                                errorMessage = err ?: "Could not send reset email."
                            }
                        }
                    }
                ) {
                    Text("Send Email")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun validateAndSubmitSignIn(
    email: String,
    pass: String,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
    onSubmit: (String, String, (Boolean, String?) -> Unit) -> Unit
) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
        onError("Please enter a valid email address.")
        return
    }
    if (pass.isBlank() || pass.length < 6) {
        onError("Password must be at least 6 characters.")
        return
    }

    onLoading(true)
    onSubmit(cleanEmail, pass) { success, error ->
        onLoading(false)
        if (success) {
            onSuccess("Signed in successfully! Personalized storage synced.")
        } else {
            onError(error ?: "Sign-in failed. Please verify credentials.")
        }
    }
}

private fun validateAndSubmitSignUp(
    email: String,
    pass: String,
    confirmPass: String,
    displayName: String,
    onLoading: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit,
    onSubmit: (String, String, String, (Boolean, String?) -> Unit) -> Unit
) {
    val cleanEmail = email.trim()
    if (cleanEmail.isBlank() || !cleanEmail.contains("@") || !cleanEmail.contains(".")) {
        onError("Please enter a valid email address.")
        return
    }
    if (pass.isBlank() || pass.length < 6) {
        onError("Password must be at least 6 characters.")
        return
    }
    if (pass != confirmPass) {
        onError("Passwords do not match.")
        return
    }

    onLoading(true)
    onSubmit(cleanEmail, pass, displayName) { success, error ->
        onLoading(false)
        if (success) {
            onSuccess("Account created successfully! Cloud storage activated.")
        } else {
            onError(error ?: "Registration failed.")
        }
    }
}

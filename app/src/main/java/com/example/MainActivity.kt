package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.AuthSessionManager
import com.example.data.repository.TallyKhataRepository
import com.example.ui.AuthScreen
import com.example.ui.MainScreen
import com.example.ui.TallyKhataViewModel
import com.example.ui.TallyKhataViewModelFactory
import com.example.ui.theme.TallyKhataTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

import android.content.Intent
import com.example.notification.BackupReminderHelper
import com.example.ui.MainTab

class MainActivity : ComponentActivity() {
    private var pendingDestination: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BackupReminderHelper.createNotificationChannel(applicationContext)

        // Ensure default reminder is scheduled if enabled
        val reminderSettings = BackupReminderHelper.getSettings(applicationContext)
        if (reminderSettings.isEnabled && reminderSettings.nextScheduledMillis == 0L) {
            BackupReminderHelper.scheduleNextReminder(applicationContext, reminderSettings.frequencyDays)
        }

        pendingDestination = intent?.getStringExtra(BackupReminderHelper.EXTRA_DESTINATION)

        enableEdgeToEdge()
        setContent {
            TallyKhataTheme {
                AppContent(initialDestination = pendingDestination)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDestination = intent.getStringExtra(BackupReminderHelper.EXTRA_DESTINATION)
    }
}

@Composable
fun AppContent(initialDestination: String? = null) {
    val context = LocalContext.current
    val authSessionManager = remember { AuthSessionManager(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var localUser by remember { mutableStateOf(authSessionManager.getCurrentUser()) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val isLoggedIn = currentUser != null || localUser != null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!isLoggedIn) {
            AuthScreen(
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    localUser = authSessionManager.getCurrentUser()
                }
            )
        } else {
            val userKey = currentUser?.uid ?: localUser?.userId ?: "local_merchant"
            val repository = remember(userKey) {
                TallyKhataRepository(context.applicationContext)
            }
            val viewModel: TallyKhataViewModel = viewModel(
                key = userKey,
                factory = TallyKhataViewModelFactory(repository, context.applicationContext)
            )

            LaunchedEffect(userKey) {
                if (localUser != null) {
                    viewModel.syncRegistrationDetails(
                        defaultName = localUser?.storeName,
                        defaultPhone = localUser?.phone,
                        defaultEmail = localUser?.email
                    )
                }
            }

            LaunchedEffect(initialDestination) {
                if (initialDestination == BackupReminderHelper.DESTINATION_CSV_BACKUP) {
                    viewModel.selectTab(MainTab.SETTINGS)
                }
            }

            MainScreen(
                viewModel = viewModel,
                onSignOutClicked = {
                    authSessionManager.signOut()
                    localUser = null
                    currentUser = null
                }
            )
        }
    }
}

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.StudioProjectRepository
import com.example.ui.StudioScreen
import com.example.ui.StudioViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import com.example.ui.theme.MyApplicationTheme
import com.example.util.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppNavigation(intent = intent)
        }
    }
}

@Composable
fun AppNavigation(intent: android.content.Intent? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    val studioViewModel: StudioViewModel = viewModel(
        factory = remember {
            val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            val repo = StudioProjectRepository(db)
            StudioViewModel.provideFactory(repo, Firebase.auth.currentUser?.uid)
        }
    )

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            studioViewModel.setAuthenticatedUser(auth.currentUser?.uid)
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    // Handle incoming image from Android Share Intent (ACTION_SEND)
    LaunchedEffect(intent) {
        if (intent?.action == android.content.Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            val imageUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(android.content.Intent.EXTRA_STREAM) as? android.net.Uri
            }
            if (imageUri != null) {
                studioViewModel.importImageFromUri(context, imageUri, asBackground = false)
            }
        }
    }

    // Non-blocking silent check for existing authorized Google account
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    studioViewModel.setAuthenticatedUser(Firebase.auth.currentUser?.uid)
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val currentThemeMode by studioViewModel.themeMode.collectAsStateWithLifecycle()

    MyApplicationTheme(themeMode = currentThemeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            StudioScreen(
                viewModel = studioViewModel,
                isUserSignedIn = currentUser != null,
                userEmailOrName = currentUser?.email ?: currentUser?.displayName,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    studioViewModel.setAuthenticatedUser(Firebase.auth.currentUser?.uid)
                },
                onSignOut = {
                    signOutUser(
                        context = context,
                        onSignOutComplete = {
                            currentUser = null
                            studioViewModel.setAuthenticatedUser(null)
                        },
                        scope = scope
                    )
                }
            )
        }
    }
}

package pant.com.nousguard

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pant.com.nousguard.security.BiometricAuthenticator
import pant.com.nousguard.ui.screens.ChatScreen
import pant.com.nousguard.ui.screens.JournalListScreen
import pant.com.nousguard.ui.theme.NousGuardTheme
import pant.com.nousguard.ui.viewmodels.JournalViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : FragmentActivity() {

    private lateinit var biometricAuthenticator: BiometricAuthenticator
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 👇 Ensure system bars (status/nav) don't interfere with Compose padding
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val application = application as NousGuardApplication

        biometricAuthenticator = BiometricAuthenticator(
            context = this,
            executor = executor,
            onAuthSuccess = {
                Log.d("BiometricAuth", "Authentication Succeeded! Loading UI.")
                loadAppUi(application)
            },
            onAuthError = { errorCode, errString ->
                Log.e("BiometricAuth", "Authentication Error ($errorCode): $errString")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Authentication Error: $errString",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            },
            onAuthFailed = {
                Log.w("BiometricAuth", "Authentication Failed! User can try again.")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Authentication Failed. Try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onBiometricNotAvailable = {
                Log.d("BiometricAuth", "Biometric auth not available, continuing...")
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Biometric not set up. Please enable fingerprint or face unlock.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                loadAppUi(application)
            }
        )

        biometricAuthenticator.authenticate(
            this,
            title = "Unlock NousGuard",
            subtitle = "Authenticate to access your private assistant.",
            description = "Use fingerprint or face recognition to proceed."
        )
    }

    private fun loadAppUi(application: NousGuardApplication) {
        lifecycleScope.launch {
            var initialized = false
            while (!initialized) {
                try {
                    application.journalRepository // ensure repo is ready
                    initialized = true
                } catch (e: UninitializedPropertyAccessException) {
                    delay(100)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Repo init error: ${e.message}", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "App error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    break
                }
            }

            setContent {
                NousGuardTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "chat_screen") {
                            composable("chat_screen") {
                                ChatScreen(navController = navController)
                            }
                            composable("journal_screen") {
                                val journalViewModel: JournalViewModel = viewModel(
                                    factory = JournalViewModel.provideFactory(application.journalRepository)
                                )
                                JournalListScreen(journalViewModel = journalViewModel)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}

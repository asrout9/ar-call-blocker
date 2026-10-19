package com.asrou.arcallblocker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.asrou.arcallblocker.security.AppLockManager
import com.asrou.arcallblocker.security.BiometricAuthManager
import com.asrou.arcallblocker.ui.navigation.AppNavigation
import com.asrou.arcallblocker.ui.security.LockScreen
import com.asrou.arcallblocker.ui.theme.ARCallBlockerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity host for the Compose UI.
 *
 * Uses [FragmentActivity] for BiometricPrompt compatibility and
 * handles Android 12+ SplashScreen initialization and App Lock gating.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install Android 12+ Splash Screen before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appLockManager.initialize()

        setContent {
            ARCallBlockerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isLocked by appLockManager.isLocked.collectAsState()

                    Crossfade(
                        targetState = isLocked,
                        label = "AppLockTransition"
                    ) { locked ->
                        if (locked) {
                            LockScreen(
                                biometricAuthManager = biometricAuthManager,
                                onUnlocked = { appLockManager.unlock() }
                            )
                        } else {
                            AppNavigation()
                        }
                    }
                }
            }
        }
    }
}

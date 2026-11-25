package com.example

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppLanguage
import com.example.model.AppThemeMode
import com.example.model.ScreenRoute
import com.example.ui.FluentGrooveApp
import com.example.ui.theme.FluentGrooveTheme
import com.example.viewmodel.GrooveViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val grooveViewModel: GrooveViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            val uiState by grooveViewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = when (uiState.themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }
            val isFullBleedDarkScreen = uiState.isMiniPlayer ||
                (uiState.route is ScreenRoute.NowPlaying && (uiState.currentTrack != null || uiState.queue.isNotEmpty()))
            val useDarkStatusBar = isDarkTheme || isFullBleedDarkScreen
            val useLightNavBar = !isDarkTheme && uiState.route is ScreenRoute.NowPlaying && !isFullBleedDarkScreen

            LaunchedEffect(useDarkStatusBar, useLightNavBar) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkStatusBar) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (useLightNavBar) {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    },
                )
            }

            val baseContext = LocalContext.current
            val baseConfig = LocalConfiguration.current
            val localizedContext = remember(baseContext, baseConfig, uiState.appLanguage) {
                when (uiState.appLanguage) {
                    AppLanguage.SYSTEM -> baseContext
                    AppLanguage.ENGLISH -> {
                        val config = Configuration(baseConfig)
                        config.setLocale(Locale.ENGLISH)
                        baseContext.createConfigurationContext(config)
                    }
                }
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalActivityResultRegistryOwner provides this@MainActivity,
                LocalOnBackPressedDispatcherOwner provides this@MainActivity,
            ) {
                FluentGrooveTheme(
                    darkTheme = isDarkTheme,
                    accentColor = uiState.accentColor,
                ) {
                    FluentGrooveApp(viewModel = grooveViewModel)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        grooveViewModel.onAppBackgrounded()
    }
}

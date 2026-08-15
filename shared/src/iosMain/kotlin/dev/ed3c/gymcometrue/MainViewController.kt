package dev.ed3c.gymcometrue

import androidx.compose.ui.window.ComposeUIViewController
import dev.ed3c.gymcometrue.ui.GymComeTrueApp

fun MainViewController() = ComposeUIViewController {
    GymComeTrueApp(platformName = "iOS")
}

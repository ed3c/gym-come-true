package dev.ed3c.gymcometrue.web

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import dev.ed3c.gymcometrue.ui.GymComeTrueApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        GymComeTrueApp(platformName = "Web")
    }
}

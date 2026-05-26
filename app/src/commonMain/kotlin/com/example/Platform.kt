package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

// Wraps the two platform-specific operations the Compose tree needs:
// sharing text via the OS share sheet, and showing a brief toast/alert.
interface Platform {
    fun share(text: String)
    fun toast(message: String)
}

val LocalPlatform = staticCompositionLocalOf<Platform> {
    error("No Platform provided")
}

// Convenience composable to provide a Platform to the tree.
@Composable
expect fun rememberPlatform(): Platform

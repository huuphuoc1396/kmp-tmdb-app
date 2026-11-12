package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Hosts [App] in iosApp. Named like a type, as Compose Multiplatform and architecture 11 do, for the Swift side. */
@Suppress("FunctionNaming")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }

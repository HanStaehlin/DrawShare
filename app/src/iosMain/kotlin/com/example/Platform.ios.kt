package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication

internal class IosPlatform : Platform {
    override fun share(text: String) {
        val vc = UIApplication.sharedApplication().keyWindow?.rootViewController ?: return
        val activityVC = UIActivityViewController(listOf(text), null)
        vc.presentViewController(activityVC, animated = true, completion = null)
    }

    override fun toast(message: String) {
        val vc = UIApplication.sharedApplication().keyWindow?.rootViewController ?: return
        val alert = UIAlertController.alertControllerWithTitle(null, message, UIAlertControllerStyleAlert)
        vc.presentViewController(alert, animated = true, completion = null)
        // Auto-dismiss after 1.5 s
        platform.darwin.dispatch_after(
            platform.darwin.dispatch_time(platform.darwin.DISPATCH_TIME_NOW, 1_500_000_000L),
            platform.darwin.dispatch_get_main_queue(),
        ) { alert.dismissViewControllerAnimated(true, completion = null) }
    }
}

@Composable
actual fun rememberPlatform(): Platform = remember { IosPlatform() }

package com.example.ui

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

internal actual fun formatTimestamp(epochMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    val fmt = NSDateFormatter()
    fmt.dateFormat = "MMM d, h:mm:ss a"
    return fmt.stringFromDate(date)
}

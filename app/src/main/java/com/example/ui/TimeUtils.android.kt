package com.example.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal actual fun formatTimestamp(epochMillis: Long): String =
    SimpleDateFormat("MMM d, h:mm:ss a", Locale.getDefault()).format(Date(epochMillis))

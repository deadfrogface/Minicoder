package com.minicode

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun copyToClipboard(context: Context, text: String) {
    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(
        ClipData.newPlainText("minicode", text)
    )
}

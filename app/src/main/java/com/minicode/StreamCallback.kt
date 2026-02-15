package com.minicode

/**
 * Callback for token-by-token streaming from native. Used by JNI.
 * onToken is invoked on the native thread; post to main thread in the receiver.
 * isCancelled() is polled by native each token to support cancel.
 */
class StreamCallback(private val onTokenReceiver: (String) -> Unit) {
    @Volatile
    var cancelled = false

    /** Set to Constants.CANCELLATION_REASON_SAFETY_LIMIT when stopped by safety guard (repeat/time/char limit). */
    @Volatile
    var cancellationReason: String? = null

    fun onToken(token: String) {
        onTokenReceiver(token)
    }

    fun isCancelled(): Boolean = cancelled
}

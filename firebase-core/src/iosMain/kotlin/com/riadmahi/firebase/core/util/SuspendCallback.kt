@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.riadmahi.firebase.core.util

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridge Objective-C completion handlers to Kotlin suspend functions.
 * Converts callback-based Firebase iOS SDK calls to coroutines.
 */
suspend fun <T> awaitResult(
    block: (callback: (T?, NSError?) -> Unit) -> Unit
): T = suspendCancellableCoroutine { cont ->
    block { result, error ->
        when {
            error != null -> {
                cont.resumeWithException(error.toException())
            }
            result != null -> {
                cont.resume(result)
            }
            else -> {
                cont.resumeWithException(
                    IllegalStateException("Firebase callback returned null result without error")
                )
            }
        }
    }
}

/**
 * Bridge for void completion handlers (operations that don't return data).
 */
suspend fun awaitVoid(
    block: (callback: (NSError?) -> Unit) -> Unit
): Unit = suspendCancellableCoroutine { cont ->
    block { error ->
        if (error != null) {
            cont.resumeWithException(error.toException())
        } else {
            cont.resume(Unit)
        }
    }
}

/**
 * Bridge for optional result completion handlers.
 */
suspend fun <T> awaitOptionalResult(
    block: (callback: (T?, NSError?) -> Unit) -> Unit
): T? = suspendCancellableCoroutine { cont ->
    block { result, error ->
        if (error != null) {
            cont.resumeWithException(error.toException())
        } else {
            cont.resume(result)
        }
    }
}

/**
 * Convert NSError to Kotlin Exception.
 */
fun NSError.toException(): Exception {
    return FirebaseNativeException(
        code = this.code.toInt(),
        domain = this.domain ?: "Unknown",
        message = this.localizedDescription
    )
}

/**
 * Native Firebase exception from iOS SDK.
 */
class FirebaseNativeException(
    val code: Int,
    val domain: String,
    override val message: String
) : Exception("[$domain:$code] $message")

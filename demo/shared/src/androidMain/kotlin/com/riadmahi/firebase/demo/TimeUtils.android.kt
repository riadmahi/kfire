package com.riadmahi.firebase.demo

import kotlin.time.Clock

actual object TimeUtils {
    actual fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
    actual fun currentTimeString(): String = Clock.System.now().toString()
}

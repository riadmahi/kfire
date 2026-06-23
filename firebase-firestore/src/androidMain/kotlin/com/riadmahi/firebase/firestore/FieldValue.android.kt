package com.riadmahi.firebase.firestore

import com.google.firebase.firestore.FieldValue as AndroidFieldValue
import com.google.firebase.Timestamp as AndroidTimestamp
import kotlin.time.Instant

actual class FieldValue private constructor(
    internal val android: AndroidFieldValue
) {
    actual companion object {
        actual fun delete(): FieldValue = FieldValue(AndroidFieldValue.delete())

        actual fun serverTimestamp(): FieldValue = FieldValue(AndroidFieldValue.serverTimestamp())

        actual fun increment(value: Long): FieldValue = FieldValue(AndroidFieldValue.increment(value))

        actual fun increment(value: Double): FieldValue = FieldValue(AndroidFieldValue.increment(value))

        actual fun arrayUnion(vararg elements: Any): FieldValue =
            FieldValue(AndroidFieldValue.arrayUnion(*elements))

        actual fun arrayRemove(vararg elements: Any): FieldValue =
            FieldValue(AndroidFieldValue.arrayRemove(*elements))
    }
}

actual class Timestamp private constructor(
    internal val android: AndroidTimestamp
) {
    actual val seconds: Long get() = android.seconds
    actual val nanoseconds: Int get() = android.nanoseconds

    actual companion object {
        actual fun now(): Timestamp = Timestamp(AndroidTimestamp.now())

        actual fun fromDate(date: Instant): Timestamp {
            val seconds = date.epochSeconds
            val nanos = date.nanosecondsOfSecond
            return Timestamp(AndroidTimestamp(seconds, nanos))
        }
    }

    actual fun toDate(): Instant {
        return Instant.fromEpochSeconds(seconds, nanoseconds.toLong())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Timestamp) return false
        return seconds == other.seconds && nanoseconds == other.nanoseconds
    }

    override fun hashCode(): Int {
        var result = seconds.hashCode()
        result = 31 * result + nanoseconds
        return result
    }

    override fun toString(): String = "Timestamp(seconds=$seconds, nanoseconds=$nanoseconds)"
}

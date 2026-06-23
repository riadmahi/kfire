@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.riadmahi.firebase.firestore

import cocoapods.FirebaseFirestoreInternal.FIRFieldValue
import cocoapods.FirebaseFirestoreInternal.FIRFirestore
import cocoapods.FirebaseFirestoreInternal.FIRFirestoreSettings
import cocoapods.FirebaseFirestoreInternal.FIRMemoryCacheSettings
import cocoapods.FirebaseFirestoreInternal.FIRMemoryEagerGCSettings
import cocoapods.FirebaseFirestoreInternal.FIRMemoryLRUGCSettings
import cocoapods.FirebaseFirestoreInternal.FIRPersistentCacheSettings
import com.riadmahi.firebase.core.FirebaseApp
import com.riadmahi.firebase.core.FirebaseResult
import com.riadmahi.firebase.core.util.awaitVoid
import kotlin.time.Instant
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of FirebaseFirestore using Firebase iOS SDK.
 */
actual class FirebaseFirestore private constructor(
    internal val ios: FIRFirestore
) {
    actual fun collection(path: String): CollectionReference {
        return CollectionReference(ios.collectionWithPath(path))
    }

    actual fun document(path: String): DocumentReference {
        return DocumentReference(ios.documentWithPath(path))
    }

    actual fun collectionGroup(collectionId: String): Query {
        return Query(ios.collectionGroupWithID(collectionId))
    }

    actual suspend fun <T> runTransaction(
        block: suspend Transaction.() -> T
    ): FirebaseResult<T> = safeFirestoreCall {
        var result: T? = null
        var transactionError: Exception? = null

        kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            ios.runTransactionWithBlock(
                { firTransaction, _ ->
                    try {
                        val transaction = Transaction(firTransaction!!)
                        result = kotlinx.coroutines.runBlocking { block(transaction) }
                        null // Return null to indicate success
                    } catch (e: Exception) {
                        transactionError = e
                        null
                    }
                },
                completion = { _, error ->
                    if (error != null) {
                        continuation.resumeWith(Result.failure(error.toException()))
                    } else if (transactionError != null) {
                        continuation.resumeWith(Result.failure(transactionError!!))
                    } else {
                        continuation.resumeWith(Result.success(Unit))
                    }
                }
            )
        }
        result!!
    }

    actual fun batch(): WriteBatch {
        return WriteBatch(ios.batch())
    }

    actual suspend fun clearPersistence(): FirebaseResult<Unit> = safeFirestoreCall {
        awaitVoid { callback ->
            ios.clearPersistenceWithCompletion { error ->
                callback(error)
            }
        }
    }

    actual suspend fun terminate(): FirebaseResult<Unit> = safeFirestoreCall {
        awaitVoid { callback ->
            ios.terminateWithCompletion { error ->
                callback(error)
            }
        }
    }

    actual suspend fun waitForPendingWrites(): FirebaseResult<Unit> = safeFirestoreCall {
        awaitVoid { callback ->
            ios.waitForPendingWritesWithCompletion { error ->
                callback(error)
            }
        }
    }

    actual fun useEmulator(host: String, port: Int) {
        ios.useEmulatorWithHost(host, port.toLong())
    }

    actual var settings: FirestoreSettings
        get() = ios.settings.toCommon()
        set(value) {
            ios.settings = value.toIos()
        }

    actual companion object {
        actual fun getInstance(): FirebaseFirestore {
            val firestore = FIRFirestore.firestore()
            return FirebaseFirestore(firestore)
        }

        @Suppress("UNCHECKED_CAST")
        actual fun getInstance(app: FirebaseApp): FirebaseFirestore {
            // Cast needed because cinterop generates different types for the same FIRApp
            val firestore = FIRFirestore.firestoreForApp(app.ios as objcnames.classes.FIRApp)
            return FirebaseFirestore(firestore)
        }
    }
}

/**
 * Safe wrapper for Firestore operations.
 */
internal suspend fun <T> safeFirestoreCall(block: suspend () -> T): FirebaseResult<T> {
    return try {
        FirebaseResult.Success(block())
    } catch (e: Exception) {
        FirebaseResult.Failure(e.toFirestoreException())
    }
}

/**
 * Convert Exception to FirestoreException.
 */
internal fun Exception.toFirestoreException(): FirestoreException {
    val message = this.message ?: "Unknown error"
    return when {
        message.contains("NOT_FOUND") || message.contains("5") -> FirestoreException.NotFound(message, this)
        message.contains("PERMISSION_DENIED") || message.contains("7") -> FirestoreException.PermissionDenied(message, this)
        message.contains("ALREADY_EXISTS") || message.contains("6") -> FirestoreException.AlreadyExists(message, this)
        message.contains("UNAVAILABLE") || message.contains("14") -> FirestoreException.Unavailable(message, this)
        message.contains("CANCELLED") || message.contains("1") -> FirestoreException.Cancelled(message, this)
        message.contains("INVALID_ARGUMENT") || message.contains("3") -> FirestoreException.InvalidArgument(message, this)
        message.contains("DEADLINE_EXCEEDED") || message.contains("4") -> FirestoreException.DeadlineExceeded(message, this)
        message.contains("RESOURCE_EXHAUSTED") || message.contains("8") -> FirestoreException.ResourceExhausted(message, this)
        message.contains("FAILED_PRECONDITION") || message.contains("9") -> FirestoreException.FailedPrecondition(message, this)
        message.contains("ABORTED") || message.contains("10") -> FirestoreException.Aborted(message, this)
        message.contains("OUT_OF_RANGE") || message.contains("11") -> FirestoreException.OutOfRange(message, this)
        message.contains("UNIMPLEMENTED") || message.contains("12") -> FirestoreException.Unimplemented(message, this)
        message.contains("INTERNAL") || message.contains("13") -> FirestoreException.Internal(message, this)
        message.contains("DATA_LOSS") || message.contains("15") -> FirestoreException.DataLoss(message, this)
        message.contains("UNAUTHENTICATED") || message.contains("16") -> FirestoreException.Unauthenticated(message, this)
        else -> FirestoreException.Unknown(message, this)
    }
}

internal fun platform.Foundation.NSError.toException(): Exception {
    return Exception("[$domain:$code] $localizedDescription")
}

/**
 * Convert FIRFirestoreSettings to FirestoreSettings.
 */
internal fun FIRFirestoreSettings.toCommon(): FirestoreSettings {
    // Simplified - iOS SDK doesn't expose detailed cache settings through cinterop
    return FirestoreSettings(
        host = this.host,
        sslEnabled = this.sslEnabled,
        cacheSettings = LocalCacheSettings.Persistent()
    )
}

/**
 * Convert FirestoreSettings to FIRFirestoreSettings.
 */
internal fun FirestoreSettings.toIos(): FIRFirestoreSettings {
    return FIRFirestoreSettings().apply {
        this.host = this@toIos.host
        this.sslEnabled = this@toIos.sslEnabled
        this.cacheSettings = this@toIos.cacheSettings.toIos()
    }
}

/**
 * Convert LocalCacheSettings to iOS cache settings.
 */
private fun LocalCacheSettings.toIos(): cocoapods.FirebaseFirestoreInternal.FIRLocalCacheSettingsProtocol {
    return when (this) {
        is LocalCacheSettings.Persistent -> FIRPersistentCacheSettings(
            sizeBytes = platform.Foundation.NSNumber(long = sizeBytes)
        )
        is LocalCacheSettings.Memory -> FIRMemoryCacheSettings(
            garbageCollectorSettings = garbageCollectorSettings.toIos()
        )
    }
}

/**
 * Convert MemoryGarbageCollectorSettings to iOS garbage collector settings.
 */
private fun MemoryGarbageCollectorSettings.toIos(): cocoapods.FirebaseFirestoreInternal.FIRMemoryGarbageCollectorSettingsProtocol {
    return when (this) {
        is MemoryGarbageCollectorSettings.Eager -> FIRMemoryEagerGCSettings()
        is MemoryGarbageCollectorSettings.Lru -> FIRMemoryLRUGCSettings(
            sizeBytes = platform.Foundation.NSNumber(long = sizeBytes)
        )
    }
}

/**
 * iOS implementation of FieldValue using Firebase iOS SDK.
 */
actual class FieldValue private constructor(
    internal val ios: Any
) {
    actual companion object {
        actual fun delete(): FieldValue = FieldValue(FIRFieldValue.fieldValueForDelete())
        actual fun serverTimestamp(): FieldValue = FieldValue(FIRFieldValue.fieldValueForServerTimestamp())
        actual fun increment(value: Long): FieldValue = FieldValue(FIRFieldValue.fieldValueForIntegerIncrement(value))
        actual fun increment(value: Double): FieldValue = FieldValue(FIRFieldValue.fieldValueForDoubleIncrement(value))
        actual fun arrayUnion(vararg elements: Any): FieldValue = FieldValue(FIRFieldValue.fieldValueForArrayUnion(elements.toList()))
        actual fun arrayRemove(vararg elements: Any): FieldValue = FieldValue(FIRFieldValue.fieldValueForArrayRemove(elements.toList()))
    }
}

/**
 * iOS implementation of Timestamp.
 * Note: FIRTimestamp is not exposed through cinterop, so we use a simple data class.
 */
actual class Timestamp internal constructor(
    actual val seconds: Long,
    actual val nanoseconds: Int
) {
    /**
     * Internal representation for iOS interop - converts to NSDate for Firestore operations.
     */
    internal fun toNSDate(): NSDate {
        val timeInterval = seconds.toDouble() + nanoseconds.toDouble() / 1_000_000_000.0
        // NSDate uses timeIntervalSinceReferenceDate which is Jan 1, 2001
        // timeIntervalSince1970 is Jan 1, 1970
        // Difference is 978307200 seconds
        return NSDate(timeIntervalSinceReferenceDate = timeInterval - 978307200.0)
    }

    actual companion object {
        actual fun now(): Timestamp {
            val now = NSDate()
            val totalSeconds = now.timeIntervalSince1970
            val secs = totalSeconds.toLong()
            val nanos = ((totalSeconds - secs) * 1_000_000_000).toInt()
            return Timestamp(secs, nanos)
        }

        actual fun fromDate(date: Instant): Timestamp {
            return Timestamp(date.epochSeconds, date.nanosecondsOfSecond)
        }

        /**
         * Creates a Timestamp from an NSDate.
         */
        internal fun fromNSDate(date: NSDate): Timestamp {
            val totalSeconds = date.timeIntervalSince1970
            val secs = totalSeconds.toLong()
            val nanos = ((totalSeconds - secs) * 1_000_000_000).toInt()
            return Timestamp(secs, nanos)
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

package com.riadmahi.firebase.firestore

/**
 * Sentinel values for Firestore field operations.
 */
expect class FieldValue {
    companion object {
        /**
         * Returns a sentinel for deleting a field.
         */
        fun delete(): FieldValue

        /**
         * Returns a sentinel for server timestamp.
         */
        fun serverTimestamp(): FieldValue

        /**
         * Returns a sentinel for incrementing a numeric field.
         */
        fun increment(value: Long): FieldValue

        /**
         * Returns a sentinel for incrementing a numeric field.
         */
        fun increment(value: Double): FieldValue

        /**
         * Returns a sentinel for adding elements to an array.
         */
        fun arrayUnion(vararg elements: Any): FieldValue

        /**
         * Returns a sentinel for removing elements from an array.
         */
        fun arrayRemove(vararg elements: Any): FieldValue
    }
}

/**
 * Represents a Firestore Timestamp.
 */
expect class Timestamp {
    val seconds: Long
    val nanoseconds: Int

    companion object {
        fun now(): Timestamp
        fun fromDate(date: kotlin.time.Instant): Timestamp
    }

    fun toDate(): kotlin.time.Instant
}

/**
 * Represents a geographical point.
 */
data class GeoPoint(
    val latitude: Double,
    val longitude: Double
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }
}

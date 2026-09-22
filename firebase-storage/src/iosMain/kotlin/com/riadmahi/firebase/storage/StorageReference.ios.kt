@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.riadmahi.firebase.storage

import cocoapods.FirebaseStorage.FIRStorageReference
import cocoapods.FirebaseStorage.FIRStorageMetadata
import cocoapods.FirebaseStorage.FIRStorageListResult
import com.riadmahi.firebase.core.FirebaseResult
import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class StorageReference internal constructor(
    internal val ios: FIRStorageReference
) {
    actual val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance()

    actual val name: String
        get() = ios.name()

    actual val path: String
        get() = ios.fullPath()

    actual val bucket: String
        get() = ios.bucket()

    actual val parent: StorageReference?
        get() = ios.parent()?.let { StorageReference(it) }

    actual val root: StorageReference
        get() = StorageReference(ios.root())

    actual fun child(path: String): StorageReference =
        StorageReference(ios.child(path))

    actual suspend fun putBytes(bytes: ByteArray, metadata: StorageMetadata?): FirebaseResult<UploadResult> =
        suspendCoroutine { continuation ->
            val nsData = bytes.toNSData()
            val iosMetadata = metadata?.toIos()

            ios.putData(nsData, iosMetadata) { resultMetadata, error ->
                if (error != null) {
                    continuation.resume(FirebaseResult.Failure(error.toStorageException()))
                } else {
                    continuation.resume(FirebaseResult.Success(
                        UploadResult(
                            bytesTransferred = resultMetadata?.size() ?: 0,
                            totalBytes = resultMetadata?.size() ?: 0,
                            metadata = resultMetadata?.toCommon()
                        )
                    ))
                }
            }
        }

    actual suspend fun getBytes(maxSize: Long): FirebaseResult<ByteArray> =
        suspendCoroutine { continuation ->
            ios.dataWithMaxSize(maxSize) { data, error ->
                if (error != null) {
                    continuation.resume(FirebaseResult.Failure(error.toStorageException()))
                } else if (data != null) {
                    continuation.resume(FirebaseResult.Success(data.toByteArray()))
                } else {
                    continuation.resume(FirebaseResult.Failure(
                        StorageException.Unknown("No data returned")
                    ))
                }
            }
        }

    actual suspend fun getDownloadUrl(): FirebaseResult<String> =
        suspendCoroutine { continuation ->
            ios.downloadURLWithCompletion { url, error ->
                if (error != null) {
                    continuation.resume(FirebaseResult.Failure(error.toStorageException()))
                } else if (url != null) {
                    continuation.resume(FirebaseResult.Success(url.absoluteString ?: ""))
                } else {
                    continuation.resume(FirebaseResult.Failure(
                        StorageException.Unknown("No URL returned")
                    ))
                }
            }
        }

    actual suspend fun delete(): FirebaseResult<Unit> =
        suspendCoroutine { continuation ->
            ios.deleteWithCompletion { error ->
                if (error != null) {
                    continuation.resume(FirebaseResult.Failure(error.toStorageException()))
                } else {
                    continuation.resume(FirebaseResult.Success(Unit))
                }
            }
        }

    actual suspend fun getMetadata(): FirebaseResult<StorageMetadata> =
        suspendCoroutine { continuation ->
            ios.metadataWithCompletion { metadata, error ->
                if (error != null) {
                    continuation.resume(FirebaseResult.Failure(error.toStorageException()))
                } else if (metadata != null) {
                    continuation.resume(FirebaseResult.Success(metadata.toCommon()))
                } else {
                    continuation.resume(FirebaseResult.Failure(
                        StorageException.Unknown("No metadata returned")
                    ))
                }
            }
        }

    actual suspend fun updateMetadata(metadata: StorageMetadata): FirebaseResult<StorageMetadata> =
        suspendCoroutine { continuation ->
            ios.updateMetadata(metadata.toIos()) { resultMetadata, error ->
                if (error != null) {
                    continuation.resume(FirebaseResult.Failure(error.toStorageException()))
                } else if (resultMetadata != null) {
                    continuation.resume(FirebaseResult.Success(resultMetadata.toCommon()))
                } else {
                    continuation.resume(FirebaseResult.Failure(
                        StorageException.Unknown("No metadata returned")
                    ))
                }
            }
        }

    actual suspend fun list(maxResults: Int): FirebaseResult<ListResult> =
        suspendCoroutine { continuation ->
            ios.listWithMaxResults(maxResults.toLong()) { result, error ->
                if (error != null) {
                    continuation.resume(FirebaseResult.Failure(error.toStorageException()))
                } else if (result != null) {
                    continuation.resume(FirebaseResult.Success(result.toCommon()))
                } else {
                    continuation.resume(FirebaseResult.Failure(
                        StorageException.Unknown("No result returned")
                    ))
                }
            }
        }

    actual suspend fun listAll(): FirebaseResult<ListResult> =
        suspendCoroutine { continuation ->
            ios.listAllWithCompletion { result, error ->
                if (error != null) {
                    continuation.resume(FirebaseResult.Failure(error.toStorageException()))
                } else if (result != null) {
                    continuation.resume(FirebaseResult.Success(result.toCommon()))
                } else {
                    continuation.resume(FirebaseResult.Failure(
                        StorageException.Unknown("No result returned")
                    ))
                }
            }
        }
}

private fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = this@toNSData.size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length)
        }
    }
}

private fun StorageMetadata.toIos(): FIRStorageMetadata {
    return FIRStorageMetadata().apply {
        this@toIos.contentType?.let { setContentType(it) }
        this@toIos.cacheControl?.let { setCacheControl(it) }
        this@toIos.contentDisposition?.let { setContentDisposition(it) }
        this@toIos.contentEncoding?.let { setContentEncoding(it) }
        this@toIos.contentLanguage?.let { setContentLanguage(it) }
        if (this@toIos.customMetadata.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            setCustomMetadata(this@toIos.customMetadata as Map<Any?, *>)
        }
    }
}

private fun FIRStorageMetadata.toCommon(): StorageMetadata {
    @Suppress("UNCHECKED_CAST")
    val customMeta = (customMetadata() as? Map<String, String>) ?: emptyMap()

    return StorageMetadata(
        contentType = contentType(),
        cacheControl = cacheControl(),
        contentDisposition = contentDisposition(),
        contentEncoding = contentEncoding(),
        contentLanguage = contentLanguage(),
        customMetadata = customMeta,
        name = name(),
        path = path(),
        bucket = bucket(),
        generation = generation()?.toString(),
        metageneration = metageneration()?.toString(),
        sizeBytes = size(),
        creationTimeMillis = timeCreated()?.let { (it.timeIntervalSince1970 * 1000).toLong() } ?: 0,
        updatedTimeMillis = updated()?.let { (it.timeIntervalSince1970 * 1000).toLong() } ?: 0,
        md5Hash = md5Hash()
    )
}

private fun FIRStorageListResult.toCommon(): ListResult {
    @Suppress("UNCHECKED_CAST")
    val itemsList = (items() as? List<FIRStorageReference>) ?: emptyList()
    @Suppress("UNCHECKED_CAST")
    val prefixesList = (prefixes() as? List<FIRStorageReference>) ?: emptyList()

    return ListResult(
        items = itemsList.map { StorageReference(it) },
        prefixes = prefixesList.map { StorageReference(it) },
        pageToken = pageToken()
    )
}

private fun NSError.toStorageException(): StorageException {
    val code = code.toInt()
    return when (code) {
        -13010 -> StorageException.ObjectNotFound(localizedDescription, null)
        -13011 -> StorageException.BucketNotFound(localizedDescription, null)
        -13012 -> StorageException.ProjectNotFound(null)
        -13013 -> StorageException.QuotaExceeded(null)
        -13020 -> StorageException.Unauthenticated(null)
        -13021 -> StorageException.Unauthorized(null)
        -13030 -> StorageException.RetryLimitExceeded(null)
        -13031 -> StorageException.InvalidChecksum(null)
        -13040 -> StorageException.Cancelled(null)
        else -> StorageException.Unknown(localizedDescription, null)
    }
}

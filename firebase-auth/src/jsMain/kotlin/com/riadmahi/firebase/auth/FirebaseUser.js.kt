package com.riadmahi.firebase.auth

import com.riadmahi.firebase.core.FirebaseResult
import kotlinx.coroutines.await

/**
 * Web (Kotlin/JS) implementation of FirebaseUser backed by the Firebase JS SDK.
 */
actual class FirebaseUser internal constructor(
    internal val js: UserJs
) {
    actual val uid: String get() = js.uid
    actual val email: String? get() = js.email
    actual val displayName: String? get() = js.displayName
    actual val photoUrl: String? get() = js.photoURL
    actual val phoneNumber: String? get() = js.phoneNumber
    actual val isAnonymous: Boolean get() = js.isAnonymous
    actual val isEmailVerified: Boolean get() = js.emailVerified
    actual val providerId: String get() = js.providerId

    actual val providerData: List<UserInfo>
        get() = js.providerData.map {
            UserInfo(
                uid = it.uid,
                providerId = it.providerId,
                email = it.email,
                displayName = it.displayName,
                photoUrl = it.photoURL,
                phoneNumber = it.phoneNumber
            )
        }

    actual suspend fun getIdToken(forceRefresh: Boolean): FirebaseResult<String> = safeAuthCall {
        js.getIdToken(forceRefresh).await()
    }

    actual suspend fun reload(): FirebaseResult<Unit> = safeAuthCall {
        reload(js).await()
    }

    actual suspend fun delete(): FirebaseResult<Unit> = safeAuthCall {
        deleteUser(js).await()
    }

    actual suspend fun updateProfile(
        displayName: String?,
        photoUrl: String?
    ): FirebaseResult<Unit> = safeAuthCall {
        val profile: dynamic = js("({})")
        if (displayName != null) profile.displayName = displayName
        if (photoUrl != null) profile.photoURL = photoUrl
        updateProfile(js, profile).await()
    }

    actual suspend fun updateEmail(email: String): FirebaseResult<Unit> = safeAuthCall {
        updateEmail(js, email).await()
    }

    actual suspend fun updatePassword(password: String): FirebaseResult<Unit> = safeAuthCall {
        updatePassword(js, password).await()
    }

    actual suspend fun sendEmailVerification(): FirebaseResult<Unit> = safeAuthCall {
        sendEmailVerification(js).await()
    }

    actual suspend fun linkWithCredential(credential: AuthCredential): FirebaseResult<AuthResult> =
        FirebaseResult.Failure(
            AuthException.OperationNotAllowed("linkWithCredential is not implemented in the web PoC")
        )

    actual suspend fun unlink(providerId: String): FirebaseResult<FirebaseUser> =
        FirebaseResult.Failure(
            AuthException.OperationNotAllowed("unlink is not implemented in the web PoC")
        )

    actual suspend fun reauthenticate(credential: AuthCredential): FirebaseResult<Unit> =
        FirebaseResult.Failure(
            AuthException.OperationNotAllowed("reauthenticate is not implemented in the web PoC")
        )
}

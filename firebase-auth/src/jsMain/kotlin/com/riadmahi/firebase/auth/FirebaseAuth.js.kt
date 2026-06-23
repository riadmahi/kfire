package com.riadmahi.firebase.auth

import com.riadmahi.firebase.core.FirebaseApp
import com.riadmahi.firebase.core.FirebaseResult
import kotlinx.coroutines.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Web (Kotlin/JS) implementation of FirebaseAuth backed by the Firebase JS SDK.
 */
actual class FirebaseAuth internal constructor(
    internal val js: AuthJs
) {
    actual val currentUser: FirebaseUser?
        get() = js.currentUser?.let { FirebaseUser(it) }

    actual val authStateFlow: Flow<FirebaseUser?>
        get() = callbackFlow {
            val unsubscribe = onAuthStateChanged(js) { user ->
                trySend(user?.let { FirebaseUser(it) })
            }
            awaitClose { unsubscribe() }
        }

    actual suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): FirebaseResult<AuthResult> = safeAuthCall {
        signInWithEmailAndPassword(js, email, password).await().toAuthResult()
    }

    actual suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String
    ): FirebaseResult<AuthResult> = safeAuthCall {
        createUserWithEmailAndPassword(js, email, password).await().toAuthResult()
    }

    actual suspend fun signInWithCredential(
        credential: AuthCredential
    ): FirebaseResult<AuthResult> = when (credential) {
        is AuthCredential.EmailPassword ->
            signInWithEmailAndPassword(credential.email, credential.password)
        is AuthCredential.Custom ->
            signInWithCustomToken(credential.token)
        else -> FirebaseResult.Failure(
            AuthException.OperationNotAllowed("Provider credential sign-in is not implemented in the web PoC")
        )
    }

    actual suspend fun signInAnonymously(): FirebaseResult<AuthResult> = safeAuthCall {
        signInAnonymously(js).await().toAuthResult()
    }

    actual suspend fun signInWithCustomToken(token: String): FirebaseResult<AuthResult> = safeAuthCall {
        signInWithCustomToken(js, token).await().toAuthResult()
    }

    actual suspend fun signOut(): FirebaseResult<Unit> = safeAuthCall {
        signOut(js).await()
    }

    actual suspend fun sendPasswordResetEmail(email: String): FirebaseResult<Unit> = safeAuthCall {
        sendPasswordResetEmail(js, email).await()
    }

    actual suspend fun confirmPasswordReset(code: String, newPassword: String): FirebaseResult<Unit> = safeAuthCall {
        confirmPasswordReset(js, code, newPassword).await()
    }

    actual fun useEmulator(host: String, port: Int) {
        connectAuthEmulator(js, "http://$host:$port")
    }

    actual companion object {
        actual fun getInstance(): FirebaseAuth = FirebaseAuth(getAuth())

        // The JS SDK derives the Auth instance from the default app; named apps
        // are not wired through in this PoC.
        actual fun getInstance(app: FirebaseApp): FirebaseAuth = FirebaseAuth(getAuth())
    }
}

internal fun UserCredentialJs.toAuthResult(): AuthResult =
    AuthResult(user = FirebaseUser(user), additionalUserInfo = null)

internal suspend fun <T> safeAuthCall(block: suspend () -> T): FirebaseResult<T> =
    try {
        FirebaseResult.Success(block())
    } catch (t: Throwable) {
        FirebaseResult.Failure(t.toAuthException())
    }

internal fun Throwable.toAuthException(): AuthException {
    val msg = message ?: "Unknown error"
    return when {
        msg.contains("user-not-found") -> AuthException.UserNotFound(cause = this)
        msg.contains("wrong-password") -> AuthException.WrongPassword(cause = this)
        msg.contains("email-already-in-use") -> AuthException.EmailAlreadyInUse(cause = this)
        msg.contains("invalid-email") -> AuthException.InvalidEmail(cause = this)
        msg.contains("weak-password") -> AuthException.WeakPassword(cause = this)
        msg.contains("too-many-requests") -> AuthException.TooManyRequests(cause = this)
        msg.contains("requires-recent-login") -> AuthException.RequiresRecentLogin(cause = this)
        msg.contains("network") -> AuthException.NetworkError(cause = this)
        else -> AuthException.Unknown(msg, this)
    }
}

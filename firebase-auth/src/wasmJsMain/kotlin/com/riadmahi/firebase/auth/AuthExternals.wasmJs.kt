@file:Suppress("unused")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.riadmahi.firebase.auth

import kotlin.js.Promise

/** External declarations for the Firebase JS SDK `firebase/auth` module (Kotlin/Wasm). */

internal external interface AuthJs : JsAny {
    val currentUser: UserJs?
}

internal external interface UserJs : JsAny {
    val uid: String
    val email: String?
    val displayName: String?
    val photoURL: String?
    val phoneNumber: String?
    val isAnonymous: Boolean
    val emailVerified: Boolean
    val providerId: String
    val providerData: JsArray<UserInfoJs>
    fun getIdToken(forceRefresh: Boolean): Promise<JsString>
}

internal external interface UserInfoJs : JsAny {
    val uid: String
    val providerId: String
    val email: String?
    val displayName: String?
    val photoURL: String?
    val phoneNumber: String?
}

internal external interface UserCredentialJs : JsAny {
    val user: UserJs
}

@JsModule("firebase/auth")
internal external fun getAuth(): AuthJs

@JsModule("firebase/auth")
internal external fun signInWithEmailAndPassword(auth: AuthJs, email: String, password: String): Promise<UserCredentialJs>

@JsModule("firebase/auth")
internal external fun createUserWithEmailAndPassword(auth: AuthJs, email: String, password: String): Promise<UserCredentialJs>

@JsModule("firebase/auth")
internal external fun signInAnonymously(auth: AuthJs): Promise<UserCredentialJs>

@JsModule("firebase/auth")
internal external fun signInWithCustomToken(auth: AuthJs, token: String): Promise<UserCredentialJs>

@JsModule("firebase/auth")
internal external fun signOut(auth: AuthJs): Promise<JsAny?>

@JsModule("firebase/auth")
internal external fun sendPasswordResetEmail(auth: AuthJs, email: String): Promise<JsAny?>

@JsModule("firebase/auth")
internal external fun confirmPasswordReset(auth: AuthJs, code: String, newPassword: String): Promise<JsAny?>

@JsModule("firebase/auth")
internal external fun onAuthStateChanged(auth: AuthJs, next: (UserJs?) -> Unit): JsAny

@JsModule("firebase/auth")
internal external fun connectAuthEmulator(auth: AuthJs, url: String)

@JsModule("firebase/auth")
internal external fun reload(user: UserJs): Promise<JsAny?>

@JsModule("firebase/auth")
internal external fun deleteUser(user: UserJs): Promise<JsAny?>

@JsModule("firebase/auth")
internal external fun updateProfile(user: UserJs, profile: JsAny): Promise<JsAny?>

@JsModule("firebase/auth")
internal external fun updateEmail(user: UserJs, email: String): Promise<JsAny?>

@JsModule("firebase/auth")
internal external fun updatePassword(user: UserJs, password: String): Promise<JsAny?>

@JsModule("firebase/auth")
internal external fun sendEmailVerification(user: UserJs): Promise<JsAny?>

/** Invokes the unsubscribe function returned by onAuthStateChanged. */
internal fun invokeUnsubscribe(unsubscribe: JsAny): Unit = js("unsubscribe()")

/** Builds the plain JS profile object expected by updateProfile. */
internal fun profileObject(displayName: String?, photoURL: String?): JsAny =
    js("({ displayName: displayName, photoURL: photoURL })")

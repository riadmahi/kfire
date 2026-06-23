@file:Suppress("unused")

package com.riadmahi.firebase.auth

import kotlin.js.Promise

/** External declarations for the Firebase JS SDK `firebase/auth` module. */

internal external interface AuthJs {
    val currentUser: UserJs?
}

internal external interface UserJs {
    val uid: String
    val email: String?
    val displayName: String?
    val photoURL: String?
    val phoneNumber: String?
    val isAnonymous: Boolean
    val emailVerified: Boolean
    val providerId: String
    val providerData: Array<UserInfoJs>
    fun getIdToken(forceRefresh: Boolean): Promise<String>
}

internal external interface UserInfoJs {
    val uid: String
    val providerId: String
    val email: String?
    val displayName: String?
    val photoURL: String?
    val phoneNumber: String?
}

internal external interface UserCredentialJs {
    val user: UserJs
}

@JsModule("firebase/auth")
@JsNonModule
internal external fun getAuth(): AuthJs

@JsModule("firebase/auth")
@JsNonModule
internal external fun signInWithEmailAndPassword(auth: AuthJs, email: String, password: String): Promise<UserCredentialJs>

@JsModule("firebase/auth")
@JsNonModule
internal external fun createUserWithEmailAndPassword(auth: AuthJs, email: String, password: String): Promise<UserCredentialJs>

@JsModule("firebase/auth")
@JsNonModule
internal external fun signInAnonymously(auth: AuthJs): Promise<UserCredentialJs>

@JsModule("firebase/auth")
@JsNonModule
internal external fun signInWithCustomToken(auth: AuthJs, token: String): Promise<UserCredentialJs>

@JsModule("firebase/auth")
@JsNonModule
internal external fun signOut(auth: AuthJs): Promise<Unit>

@JsModule("firebase/auth")
@JsNonModule
internal external fun sendPasswordResetEmail(auth: AuthJs, email: String): Promise<Unit>

@JsModule("firebase/auth")
@JsNonModule
internal external fun confirmPasswordReset(auth: AuthJs, code: String, newPassword: String): Promise<Unit>

@JsModule("firebase/auth")
@JsNonModule
internal external fun onAuthStateChanged(auth: AuthJs, next: (UserJs?) -> Unit): () -> Unit

@JsModule("firebase/auth")
@JsNonModule
internal external fun connectAuthEmulator(auth: AuthJs, url: String)

@JsModule("firebase/auth")
@JsNonModule
internal external fun reload(user: UserJs): Promise<Unit>

@JsModule("firebase/auth")
@JsNonModule
internal external fun deleteUser(user: UserJs): Promise<Unit>

@JsModule("firebase/auth")
@JsNonModule
internal external fun updateProfile(user: UserJs, profile: dynamic): Promise<Unit>

@JsModule("firebase/auth")
@JsNonModule
internal external fun updateEmail(user: UserJs, email: String): Promise<Unit>

@JsModule("firebase/auth")
@JsNonModule
internal external fun updatePassword(user: UserJs, password: String): Promise<Unit>

@JsModule("firebase/auth")
@JsNonModule
internal external fun sendEmailVerification(user: UserJs): Promise<Unit>

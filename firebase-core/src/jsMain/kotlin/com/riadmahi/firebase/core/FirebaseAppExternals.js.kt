@file:Suppress("unused")

package com.riadmahi.firebase.core

/**
 * External declarations for the Firebase JS SDK `firebase/app` module.
 *
 * For bundling, add the npm dependency to the `jsMain` source set:
 * `implementation(npm("firebase", "<version>"))`.
 */

internal external interface FirebaseAppJs {
    val name: String
    val options: FirebaseOptionsJs
}

internal external interface FirebaseOptionsJs {
    val apiKey: String?
    val appId: String?
    val projectId: String?
    val storageBucket: String?
    val messagingSenderId: String?
    val databaseURL: String?
}

@JsModule("firebase/app")
@JsNonModule
internal external fun initializeApp(options: dynamic, name: String): FirebaseAppJs

@JsModule("firebase/app")
@JsNonModule
internal external fun getApp(name: String): FirebaseAppJs

@JsModule("firebase/app")
@JsNonModule
internal external fun getApps(): Array<FirebaseAppJs>

@JsModule("firebase/app")
@JsNonModule
internal external fun deleteApp(app: FirebaseAppJs)

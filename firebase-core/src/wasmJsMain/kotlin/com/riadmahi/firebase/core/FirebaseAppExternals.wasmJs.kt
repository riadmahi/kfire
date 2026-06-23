@file:Suppress("unused")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.riadmahi.firebase.core

/**
 * External declarations for the Firebase JS SDK `firebase/app` module (Kotlin/Wasm).
 *
 * For bundling, add the npm dependency to the `wasmJsMain` source set:
 * `implementation(npm("firebase", "<version>"))`.
 */

internal external interface FirebaseAppJs : JsAny {
    val name: String
    val options: FirebaseOptionsJs
}

internal external interface FirebaseOptionsJs : JsAny {
    val apiKey: String?
    val appId: String?
    val projectId: String?
    val storageBucket: String?
    val messagingSenderId: String?
    val databaseURL: String?
}

@JsModule("firebase/app")
internal external fun initializeApp(options: JsAny, name: String): FirebaseAppJs

@JsModule("firebase/app")
internal external fun getApp(name: String): FirebaseAppJs

@JsModule("firebase/app")
internal external fun getApps(): JsArray<FirebaseAppJs>

@JsModule("firebase/app")
internal external fun deleteApp(app: FirebaseAppJs)

internal fun firebaseConfig(
    apiKey: String,
    appId: String,
    projectId: String,
    storageBucket: String,
    messagingSenderId: String,
    databaseURL: String,
): JsAny =
    js("({ apiKey: apiKey, appId: appId, projectId: projectId, storageBucket: storageBucket, messagingSenderId: messagingSenderId, databaseURL: databaseURL })")

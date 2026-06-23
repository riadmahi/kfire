@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.riadmahi.firebase.core

/**
 * Web (Kotlin/Wasm) implementation of FirebaseApp backed by the Firebase JS SDK.
 */
actual class FirebaseApp internal constructor(
    internal val js: FirebaseAppJs
) {
    actual val name: String
        get() = js.name

    actual val options: FirebaseOptions
        get() = js.options.toCommon()

    actual fun delete() {
        deleteApp(js)
    }

    actual companion object {
        actual val DEFAULT_APP_NAME: String = "[DEFAULT]"

        actual fun initialize(): FirebaseApp {
            throw FirebaseException.NotInitializedException(
                "On web, FirebaseApp.initialize() requires explicit FirebaseOptions."
            )
        }

        actual fun initialize(options: FirebaseOptions): FirebaseApp =
            initialize(options, DEFAULT_APP_NAME)

        actual fun initialize(options: FirebaseOptions, name: String): FirebaseApp {
            val config = firebaseConfig(
                apiKey = options.apiKey,
                appId = options.applicationId,
                projectId = options.projectId,
                storageBucket = options.storageBucket ?: "",
                messagingSenderId = options.gcmSenderId ?: "",
                databaseURL = options.databaseUrl ?: "",
            )
            return FirebaseApp(initializeApp(config, name))
        }

        actual fun getInstance(): FirebaseApp = getInstance(DEFAULT_APP_NAME)

        actual fun getInstance(name: String): FirebaseApp = FirebaseApp(getApp(name))

        actual val apps: List<FirebaseApp>
            get() {
                val array = getApps()
                return buildList {
                    for (i in 0 until array.length) {
                        array[i]?.let { add(FirebaseApp(it)) }
                    }
                }
            }
    }
}

internal fun FirebaseOptionsJs.toCommon(): FirebaseOptions = FirebaseOptions(
    apiKey = apiKey ?: "",
    applicationId = appId ?: "",
    projectId = projectId ?: "",
    storageBucket = storageBucket,
    gcmSenderId = messagingSenderId,
    databaseUrl = databaseURL
)

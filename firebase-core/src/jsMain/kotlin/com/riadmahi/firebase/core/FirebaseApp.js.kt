package com.riadmahi.firebase.core

/**
 * Web (Kotlin/JS) implementation of FirebaseApp backed by the Firebase JS SDK.
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
            // The Firebase JS SDK has no config-file auto-initialization like the
            // native platforms; the web app must pass explicit options.
            throw FirebaseException.NotInitializedException(
                "On web, FirebaseApp.initialize() requires explicit FirebaseOptions."
            )
        }

        actual fun initialize(options: FirebaseOptions): FirebaseApp =
            initialize(options, DEFAULT_APP_NAME)

        actual fun initialize(options: FirebaseOptions, name: String): FirebaseApp {
            val config: dynamic = js("({})")
            config.apiKey = options.apiKey
            config.appId = options.applicationId
            config.projectId = options.projectId
            config.storageBucket = options.storageBucket
            config.messagingSenderId = options.gcmSenderId
            config.databaseURL = options.databaseUrl
            return FirebaseApp(initializeApp(config, name))
        }

        actual fun getInstance(): FirebaseApp = getInstance(DEFAULT_APP_NAME)

        actual fun getInstance(name: String): FirebaseApp = FirebaseApp(getApp(name))

        actual val apps: List<FirebaseApp>
            get() = getApps().map { FirebaseApp(it) }
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

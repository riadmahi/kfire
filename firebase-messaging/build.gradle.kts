plugins {
    id("convention.kmp-library")
    id("convention.maven-publish")
}

kotlin {
    android {
        namespace = "com.riadmahi.firebase.messaging"
        compileSdk = 36
        minSdk = 24
    }

    cocoapods {
        summary = "Firebase Messaging KMP wrapper"
        homepage = "https://github.com/riadmahi/kfire"
        ios.deploymentTarget = "15.0"
        version = "1.0.0"

        pod("FirebaseMessaging") {
            version = libs.versions.firebaseIos.get()
        }

        framework {
            baseName = "FirebaseMessaging"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":firebase-core"))
            implementation(libs.kotlinx.coroutines.core)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.firebase.messaging.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.datetime)
        }
    }
}

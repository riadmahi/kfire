plugins {
    id("convention.kmp-library")
    id("convention.maven-publish")
}

kotlin {
    android {
        namespace = "com.riadmahi.firebase.firestore"
        compileSdk = 36
        minSdk = 24
    }

    cocoapods {
        summary = "Firebase Firestore KMP wrapper"
        homepage = "https://github.com/riadmahi/kfire"
        ios.deploymentTarget = "15.0"
        version = "1.0.0"

        pod("FirebaseFirestoreInternal") {
            version = libs.versions.firebaseIos.get()
        }

        framework {
            baseName = "FirebaseFirestore"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":firebase-core"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.firebase.firestore.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

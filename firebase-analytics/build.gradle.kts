plugins {
    id("convention.kmp-library")
    id("convention.maven-publish")
}

kotlin {
    android {
        namespace = "com.riadmahi.firebase.analytics"
        compileSdk = 36
        minSdk = 24
    }

    cocoapods {
        summary = "Firebase Analytics KMP wrapper"
        homepage = "https://github.com/riadmahi/kfire"
        ios.deploymentTarget = "15.0"
        version = "1.0.0"

        pod("FirebaseAnalytics") {
            version = libs.versions.firebaseIos.get()
        }

        framework {
            baseName = "FirebaseAnalytics"
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
            implementation(libs.firebase.analytics.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("convention.kmp-library")
    id("convention.maven-publish")
}

kotlin {
    android {
        namespace = "com.riadmahi.firebase.core"
        compileSdk = 36
        minSdk = 24
    }

    js {
        browser()
        nodejs()
        binaries.library()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        binaries.library()
    }

    cocoapods {
        summary = "Firebase Core KMP wrapper"
        homepage = "https://github.com/riadmahi/kfire"
        ios.deploymentTarget = "15.0"
        version = "1.0.0"

        pod("FirebaseCore") {
            version = libs.versions.firebaseIos.get()
        }

        framework {
            baseName = "FirebaseCore"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.firebase.common.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

plugins {
    id("convention.kmp-library")
    id("convention.maven-publish")
}

kotlin {
    android {
        namespace = "com.riadmahi.firebase.storage"
        compileSdk = 36
        minSdk = 24
    }

    cocoapods {
        summary = "Firebase Storage KMP wrapper"
        homepage = "https://github.com/riadmahi/kfire"
        ios.deploymentTarget = "15.0"
        version = "1.0.0"

        pod("FirebaseStorage") {
            version = libs.versions.firebaseIos.get()
        }

        framework {
            baseName = "FirebaseStorage"
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
            implementation(libs.firebase.storage.android)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

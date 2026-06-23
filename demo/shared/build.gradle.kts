import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Disable Xcode-related tasks (not needed for library projects)
tasks.matching {
    it.name == "checkXcodeProjectConfiguration" ||
    it.name == "convertPbxprojToJson"
}.configureEach {
    enabled = false
}

kotlin {
    android {
        namespace = "com.riadmahi.firebase.demo.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Kotlin libraries
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            // Firebase KMP
            implementation(project(":firebase-core"))
            implementation(project(":firebase-auth"))
            implementation(project(":firebase-firestore"))
            implementation(project(":firebase-storage"))
            implementation(project(":firebase-messaging"))
            implementation(project(":firebase-analytics"))
            implementation(project(":firebase-remoteconfig"))
            implementation(project(":firebase-crashlytics"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

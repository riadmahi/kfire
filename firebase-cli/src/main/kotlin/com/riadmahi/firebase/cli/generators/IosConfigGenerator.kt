package com.riadmahi.firebase.cli.generators

import java.nio.file.Path
import kotlin.io.path.*

data class FirebaseIosModules(
    val core: Boolean = true,
    val auth: Boolean = false,
    val firestore: Boolean = false,
    val storage: Boolean = false,
    val messaging: Boolean = false,
    val analytics: Boolean = false,
    val remoteConfig: Boolean = false,
    val crashlytics: Boolean = false
)

class IosConfigGenerator(private val projectRoot: Path) {

    companion object {
        const val FIREBASE_IOS_VERSION = "12.15.0"
    }

    fun writeGoogleServiceInfoPlist(content: String) {
        // Find the appropriate location for GoogleService-Info.plist
        val possibleLocations = listOf(
            "demo/iosApp/iosApp/GoogleService-Info.plist",
            "demo/iosApp/GoogleService-Info.plist",
            "iosApp/iosApp/GoogleService-Info.plist",
            "iosApp/GoogleService-Info.plist"
        )

        val targetPath = possibleLocations
            .map { projectRoot.resolve(it) }
            .firstOrNull { it.parent.exists() }
            ?: projectRoot.resolve("iosApp/iosApp/GoogleService-Info.plist")

        // Create parent directories if needed
        targetPath.parent.createDirectories()

        // Write the file
        targetPath.writeText(content)
    }

    /**
     * Find the iOS app directory containing the Xcode project.
     */
    private fun findIosAppDirectory(): Path? {
        val possibleLocations = listOf(
            "demo/iosApp",
            "iosApp"
        )

        return possibleLocations
            .map { projectRoot.resolve(it) }
            .firstOrNull { dir ->
                dir.exists() && dir.listDirectoryEntries("*.xcodeproj").isNotEmpty()
            }
    }

    /**
     * Generate or update the Podfile with Firebase dependencies.
     * Returns the path to the Podfile if created/updated, null otherwise.
     */
    fun generatePodfile(modules: FirebaseIosModules): Path? {
        val iosAppDir = findIosAppDirectory() ?: return null
        val podfilePath = iosAppDir.resolve("Podfile")

        val pods = buildList {
            if (modules.core) add("FirebaseCore")
            if (modules.auth) add("FirebaseAuth")
            if (modules.firestore) add("FirebaseFirestoreInternal")
            if (modules.storage) add("FirebaseStorage")
            if (modules.messaging) add("FirebaseMessaging")
            if (modules.analytics) add("FirebaseAnalytics")
            if (modules.remoteConfig) add("FirebaseRemoteConfig")
            if (modules.crashlytics) add("FirebaseCrashlytics")
        }

        // Detect the target name from xcodeproj
        val targetName = iosAppDir.listDirectoryEntries("*.xcodeproj")
            .firstOrNull()
            ?.fileName
            ?.toString()
            ?.removeSuffix(".xcodeproj")
            ?: "iosApp"

        val podfileContent = buildString {
            appendLine("platform :ios, '15.0'")
            appendLine()
            appendLine("target '$targetName' do")
            appendLine("  use_frameworks!")
            appendLine()
            appendLine("  # Firebase dependencies (managed by KFire CLI)")
            for (pod in pods) {
                appendLine("  pod '$pod', '$FIREBASE_IOS_VERSION'")
            }
            appendLine("end")
            appendLine()
            appendLine("post_install do |installer|")
            appendLine("  installer.pods_project.targets.each do |target|")
            appendLine("    target.build_configurations.each do |config|")
            appendLine("      config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '15.0'")
            appendLine("    end")
            appendLine("  end")
            appendLine("end")
        }

        podfilePath.writeText(podfileContent)
        return podfilePath
    }

    /**
     * Run `pod install` in the iOS app directory.
     * Returns true if successful, false otherwise.
     */
    fun runPodInstall(): Boolean {
        val iosAppDir = findIosAppDirectory() ?: return false

        return try {
            val process = ProcessBuilder("pod", "install")
                .directory(iosAppDir.toFile())
                .redirectErrorStream(true)
                .start()

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if CocoaPods is installed.
     */
    fun isCocoaPodsInstalled(): Boolean {
        return try {
            val process = ProcessBuilder("pod", "--version")
                .redirectErrorStream(true)
                .start()
            process.waitFor() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clean up CocoaPods configuration (for migration to SPM).
     * Removes Podfile, Podfile.lock, and Pods directory.
     * Returns true if cleanup was successful.
     */
    fun cleanupCocoaPods(): Boolean {
        val iosAppDir = findIosAppDirectory() ?: return false

        return try {
            // Remove Podfile
            val podfile = iosAppDir.resolve("Podfile")
            if (podfile.exists()) podfile.deleteExisting()

            // Remove Podfile.lock
            val podfileLock = iosAppDir.resolve("Podfile.lock")
            if (podfileLock.exists()) podfileLock.deleteExisting()

            // Remove Pods directory recursively
            val podsDir = iosAppDir.resolve("Pods")
            if (podsDir.exists()) {
                podsDir.toFile().deleteRecursively()
            }

            // Remove .xcworkspace (CocoaPods generates this)
            val xcworkspaces = iosAppDir.listDirectoryEntries("*.xcworkspace")
            xcworkspaces.forEach { workspace ->
                workspace.toFile().deleteRecursively()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if CocoaPods is configured in the project.
     */
    fun hasCocoaPodsConfig(): Boolean {
        val iosAppDir = findIosAppDirectory() ?: return false
        return iosAppDir.resolve("Podfile").exists()
    }

    /**
     * Configure Firebase initialization in the iOS app's Swift entry point.
     * Adds `import FirebaseCore` and `FirebaseApp.configure()` to the App struct.
     * Returns true if successful, false otherwise.
     */
    fun configureFirebaseInit(): Boolean {
        val iosAppDir = findIosAppDirectory() ?: return false

        // Find Swift app entry point files
        val swiftFiles = findSwiftAppFiles(iosAppDir)
        if (swiftFiles.isEmpty()) return false

        for (swiftFile in swiftFiles) {
            val content = swiftFile.readText()

            // Skip if already configured
            if (content.contains("FirebaseApp.configure()")) {
                return true
            }

            // Check if this is a SwiftUI App file
            if (content.contains("@main") && content.contains("struct") && content.contains(": App")) {
                val modified = addFirebaseInitToSwiftUIApp(content)
                if (modified != content) {
                    swiftFile.writeText(modified)
                    return true
                }
            }

            // Check if this is an AppDelegate
            if (content.contains("UIApplicationDelegate") || content.contains("AppDelegate")) {
                val modified = addFirebaseInitToAppDelegate(content)
                if (modified != content) {
                    swiftFile.writeText(modified)
                    return true
                }
            }
        }

        return false
    }

    /**
     * Find Swift files that could be app entry points.
     */
    private fun findSwiftAppFiles(iosAppDir: Path): List<Path> {
        val appNames = listOf("iOSApp.swift", "App.swift", "AppDelegate.swift")
        val result = mutableListOf<Path>()

        // Search in iosApp subdirectory and root
        val searchDirs = listOf(
            iosAppDir.resolve("iosApp"),
            iosAppDir
        )

        for (dir in searchDirs) {
            if (!dir.exists()) continue
            for (name in appNames) {
                val file = dir.resolve(name)
                if (file.exists()) {
                    result.add(file)
                }
            }
            // Also look for any file with @main
            dir.listDirectoryEntries("*.swift").forEach { file ->
                if (file.readText().contains("@main") && file !in result) {
                    result.add(file)
                }
            }
        }

        return result
    }

    /**
     * Add Firebase initialization to a SwiftUI App struct.
     */
    private fun addFirebaseInitToSwiftUIApp(content: String): String {
        var modified = content

        // Add import if not present
        if (!modified.contains("import FirebaseCore")) {
            modified = modified.replace(
                "import SwiftUI",
                "import SwiftUI\nimport FirebaseCore"
            )
        }

        // Add init() with FirebaseApp.configure() if not present
        if (!modified.contains("FirebaseApp.configure()")) {
            // Find the struct declaration and add init
            val structRegex = Regex("""(@main\s*struct\s+\w+\s*:\s*App\s*\{)""")
            modified = structRegex.replace(modified) { matchResult ->
                "${matchResult.value}\n    init() {\n        FirebaseApp.configure()\n    }\n"
            }
        }

        return modified
    }

    /**
     * Add Firebase initialization to an AppDelegate.
     */
    private fun addFirebaseInitToAppDelegate(content: String): String {
        var modified = content

        // Add import if not present
        if (!modified.contains("import FirebaseCore") && !modified.contains("import Firebase")) {
            val importRegex = Regex("""(import UIKit)""")
            modified = importRegex.replace(modified) {
                "import UIKit\nimport FirebaseCore"
            }
        }

        // Add FirebaseApp.configure() in didFinishLaunchingWithOptions if not present
        if (!modified.contains("FirebaseApp.configure()")) {
            val didFinishRegex = Regex(
                """(func application\s*\([^)]*didFinishLaunchingWithOptions[^)]*\)[^{]*\{)""",
                RegexOption.DOT_MATCHES_ALL
            )
            modified = didFinishRegex.replace(modified) { matchResult ->
                "${matchResult.value}\n        FirebaseApp.configure()"
            }
        }

        return modified
    }
}

package com.riadmahi.firebase.cli.generators

import com.riadmahi.firebase.cli.parsers.PbxprojParser
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeText

/**
 * Generator for Swift Package Manager configuration.
 * Modifies the Xcode project.pbxproj to add Firebase SPM dependencies.
 */
class SpmConfigGenerator(private val projectRoot: Path) {

    companion object {
        const val FIREBASE_REPO_URL = "https://github.com/firebase/firebase-ios-sdk.git"
        const val FIREBASE_VERSION = "12.15.0"

        /**
         * Mapping from FirebaseIosModules flags to SPM product names.
         */
        private val MODULE_TO_PRODUCT = mapOf(
            "core" to "FirebaseCore",
            "auth" to "FirebaseAuth",
            "firestore" to "FirebaseFirestore",
            "storage" to "FirebaseStorage",
            "messaging" to "FirebaseMessaging",
            "analytics" to "FirebaseAnalytics",
            "remoteConfig" to "FirebaseRemoteConfig",
            "crashlytics" to "FirebaseCrashlytics"
        )
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
     * Find the project.pbxproj file.
     */
    private fun findPbxprojPath(): Path? {
        val iosAppDir = findIosAppDirectory() ?: return null
        val xcodeproj = iosAppDir.listDirectoryEntries("*.xcodeproj").firstOrNull() ?: return null
        val pbxproj = xcodeproj.resolve("project.pbxproj")
        return if (pbxproj.exists()) pbxproj else null
    }

    /**
     * Get the list of Firebase SPM product names based on selected modules.
     */
    fun getProductNames(modules: FirebaseIosModules): List<String> = buildList {
        if (modules.core) add(MODULE_TO_PRODUCT["core"]!!)
        if (modules.auth) add(MODULE_TO_PRODUCT["auth"]!!)
        if (modules.firestore) add(MODULE_TO_PRODUCT["firestore"]!!)
        if (modules.storage) add(MODULE_TO_PRODUCT["storage"]!!)
        if (modules.messaging) add(MODULE_TO_PRODUCT["messaging"]!!)
        if (modules.analytics) add(MODULE_TO_PRODUCT["analytics"]!!)
        if (modules.remoteConfig) add(MODULE_TO_PRODUCT["remoteConfig"]!!)
        if (modules.crashlytics) add(MODULE_TO_PRODUCT["crashlytics"]!!)
    }

    /**
     * Configure SPM dependencies in the Xcode project.
     * Returns true if successful, false otherwise.
     */
    fun configureSpm(modules: FirebaseIosModules): Boolean {
        val pbxprojPath = findPbxprojPath() ?: return false
        val parser = PbxprojParser(pbxprojPath)
        val document = parser.parse() ?: return false

        val products = getProductNames(modules)
        if (products.isEmpty()) return false

        val firebasePackage = SwiftPackage(
            repositoryURL = FIREBASE_REPO_URL,
            version = FIREBASE_VERSION,
            products = products
        )

        return try {
            val modifier = PbxprojModifier(document)
            val modifiedContent = modifier.addSwiftPackages(listOf(firebasePackage))
            pbxprojPath.writeText(modifiedContent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if the project already has SPM dependencies configured.
     */
    fun hasSpmDependencies(): Boolean {
        val pbxprojPath = findPbxprojPath() ?: return false
        return PbxprojParser(pbxprojPath).hasSwiftPackages()
    }

    /**
     * Get the path to the Xcode project for user information.
     */
    fun getXcodeprojPath(): Path? {
        val iosAppDir = findIosAppDirectory() ?: return null
        return iosAppDir.listDirectoryEntries("*.xcodeproj").firstOrNull()
    }

    /**
     * Clean up SPM configuration from the Xcode project (for migration to CocoaPods).
     * Removes XCRemoteSwiftPackageReference and XCSwiftPackageProductDependency sections,
     * and cleans up related references.
     * Returns true if cleanup was successful.
     */
    fun cleanupSpm(): Boolean {
        val pbxprojPath = findPbxprojPath() ?: return false

        return try {
            var content = pbxprojPath.toFile().readText()

            // Remove XCRemoteSwiftPackageReference section
            content = content.replace(
                Regex("""/\* Begin XCRemoteSwiftPackageReference section \*/.*?/\* End XCRemoteSwiftPackageReference section \*/\s*""", RegexOption.DOT_MATCHES_ALL),
                ""
            )

            // Remove XCSwiftPackageProductDependency section
            content = content.replace(
                Regex("""/\* Begin XCSwiftPackageProductDependency section \*/.*?/\* End XCSwiftPackageProductDependency section \*/\s*""", RegexOption.DOT_MATCHES_ALL),
                ""
            )

            // Remove packageReferences from PBXProject
            content = content.replace(
                Regex("""packageReferences\s*=\s*\([^)]*\);\s*"""),
                ""
            )

            // Remove packageProductDependencies from PBXNativeTarget
            content = content.replace(
                Regex("""packageProductDependencies\s*=\s*\([^)]*\);\s*"""),
                ""
            )

            // Remove SPM-related PBXBuildFile entries (those with productRef instead of fileRef)
            content = content.replace(
                Regex("""[A-F0-9]{24}\s*/\*[^*]*\*/\s*=\s*\{isa = PBXBuildFile;\s*productRef = [^}]+\};\s*"""),
                ""
            )

            pbxprojPath.writeText(content)
            true
        } catch (e: Exception) {
            false
        }
    }
}

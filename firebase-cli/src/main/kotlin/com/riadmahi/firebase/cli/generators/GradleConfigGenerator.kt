package com.riadmahi.firebase.cli.generators

import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.*

/**
 * Generator for modifying Gradle configuration files to add Firebase support.
 */
class GradleConfigGenerator(private val projectRoot: Path) {

    companion object {
        private const val GOOGLE_SERVICES_VERSION = "4.5.0"
        private const val CRASHLYTICS_VERSION = "3.0.8"
        private const val GOOGLE_SERVICES_PLUGIN_ID = "com.google.gms.google-services"
        private const val CRASHLYTICS_PLUGIN_ID = "com.google.firebase.crashlytics"

        // KFire Maven coordinates
        private const val KFIRE_GROUP_ID = "com.riadmahi.firebase"
        private const val MAVEN_METADATA_URL = "https://repo1.maven.org/maven2/com/riadmahi/firebase/firebase-core/maven-metadata.xml"
        private const val FALLBACK_VERSION = "0.1.0"

        /**
         * Fetches the latest KFire version from Maven Central.
         * Falls back to FALLBACK_VERSION if fetch fails.
         */
        fun fetchLatestVersion(): String {
            return try {
                val url = URI(MAVEN_METADATA_URL).toURL()
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                if (connection.responseCode == 200) {
                    val content = connection.inputStream.bufferedReader().readText()
                    // Parse <release> or <latest> tag from maven-metadata.xml
                    val releaseRegex = Regex("""<release>([^<]+)</release>""")
                    val latestRegex = Regex("""<latest>([^<]+)</latest>""")

                    releaseRegex.find(content)?.groupValues?.get(1)
                        ?: latestRegex.find(content)?.groupValues?.get(1)
                        ?: FALLBACK_VERSION
                } else {
                    FALLBACK_VERSION
                }
            } catch (e: Exception) {
                FALLBACK_VERSION
            }
        }
    }

    private val kfireVersion: String by lazy { fetchLatestVersion() }

    /**
     * Configuration options for Firebase modules.
     */
    data class FirebaseModules(
        val core: Boolean = true,
        val auth: Boolean = false,
        val firestore: Boolean = false,
        val storage: Boolean = false,
        val messaging: Boolean = false,
        val analytics: Boolean = false,
        val remoteConfig: Boolean = false,
        val crashlytics: Boolean = false
    )

    /**
     * Configures all Gradle files for Firebase.
     * Returns a list of modified files.
     */
    fun configureGradle(modules: FirebaseModules = FirebaseModules()): List<String> {
        val modifiedFiles = mutableListOf<String>()

        // 1. Update libs.versions.toml
        if (updateVersionCatalog(modules)) {
            modifiedFiles.add("gradle/libs.versions.toml")
        }

        // 2. Update root build.gradle.kts
        if (updateRootBuildGradle(modules)) {
            modifiedFiles.add("build.gradle.kts")
        }

        // 3. Update app build.gradle.kts
        val appBuildFile = findAppBuildFile()
        if (appBuildFile != null && updateAppBuildGradle(appBuildFile, modules)) {
            modifiedFiles.add(appBuildFile.relativeTo(projectRoot).toString())
        }

        return modifiedFiles
    }

    /**
     * Updates libs.versions.toml to add google-services, crashlytics plugins, and KFire dependencies.
     */
    private fun updateVersionCatalog(modules: FirebaseModules): Boolean {
        val versionCatalogPath = projectRoot.resolve("gradle/libs.versions.toml")
        if (!versionCatalogPath.exists()) return false

        var content = versionCatalogPath.readText()
        var modified = false

        // Ensure [libraries] section exists
        if (!content.contains("[libraries]")) {
            content = content.trimEnd() + "\n\n[libraries]\n"
            modified = true
        }

        // Ensure [plugins] section exists
        if (!content.contains("[plugins]")) {
            content = content.trimEnd() + "\n\n[plugins]\n"
            modified = true
        }

        // Add google-services version and plugin if not present
        if (!content.contains("googleServices") && !content.contains("google-services")) {
            content = addToSection(content, "[versions]", "googleServices = \"$GOOGLE_SERVICES_VERSION\"")
            content = addToSection(content, "[plugins]", "googleServices = { id = \"$GOOGLE_SERVICES_PLUGIN_ID\", version.ref = \"googleServices\" }")
            modified = true
        }

        // Add crashlytics plugin if crashlytics module is selected and not present
        if (modules.crashlytics && !content.contains("crashlyticsPlugin") && !content.contains(CRASHLYTICS_PLUGIN_ID)) {
            content = addToSection(content, "[versions]", "crashlyticsPlugin = \"$CRASHLYTICS_VERSION\"")
            content = addToSection(content, "[plugins]", "crashlyticsPlugin = { id = \"$CRASHLYTICS_PLUGIN_ID\", version.ref = \"crashlyticsPlugin\" }")
            modified = true
        }

        // Add KFire version and libraries if not present
        if (!content.contains("kfire")) {
            content = addToSection(content, "[versions]", "kfire = \"$kfireVersion\"")

            // Add selected KFire libraries
            val firebaseLibs = buildFirebaseLibraries(modules)
            for (lib in firebaseLibs) {
                content = addToSection(content, "[libraries]", lib)
            }
            modified = true
        }

        // Add coroutines if not present
        if (!content.contains("coroutines") && !content.contains("kotlinx-coroutines")) {
            content = addToSection(content, "[versions]", "coroutines = \"1.9.0\"")
            content = addToSection(content, "[libraries]", "kotlinx-coroutines-core = { group = \"org.jetbrains.kotlinx\", name = \"kotlinx-coroutines-core\", version.ref = \"coroutines\" }")
            modified = true
        }

        if (modified) {
            versionCatalogPath.writeText(content)
        }
        return modified
    }

    /**
     * Adds an entry to a specific section in the TOML file.
     */
    private fun addToSection(content: String, section: String, entry: String): String {
        val sectionIndex = content.indexOf(section)
        if (sectionIndex == -1) return content

        // Find the end of the section line
        val afterSection = content.indexOf("\n", sectionIndex)
        if (afterSection == -1) return content.trimEnd() + "\n$entry\n"

        return content.substring(0, afterSection + 1) + "$entry\n" + content.substring(afterSection + 1)
    }

    /**
     * Builds the list of KFire library entries for libs.versions.toml.
     */
    private fun buildFirebaseLibraries(modules: FirebaseModules): List<String> {
        val libs = mutableListOf<String>()
        if (modules.core) libs.add("kfire-core = { group = \"$KFIRE_GROUP_ID\", name = \"firebase-core\", version.ref = \"kfire\" }")
        if (modules.auth) libs.add("kfire-auth = { group = \"$KFIRE_GROUP_ID\", name = \"firebase-auth\", version.ref = \"kfire\" }")
        if (modules.firestore) libs.add("kfire-firestore = { group = \"$KFIRE_GROUP_ID\", name = \"firebase-firestore\", version.ref = \"kfire\" }")
        if (modules.storage) libs.add("kfire-storage = { group = \"$KFIRE_GROUP_ID\", name = \"firebase-storage\", version.ref = \"kfire\" }")
        if (modules.messaging) libs.add("kfire-messaging = { group = \"$KFIRE_GROUP_ID\", name = \"firebase-messaging\", version.ref = \"kfire\" }")
        if (modules.analytics) libs.add("kfire-analytics = { group = \"$KFIRE_GROUP_ID\", name = \"firebase-analytics\", version.ref = \"kfire\" }")
        if (modules.remoteConfig) libs.add("kfire-remoteconfig = { group = \"$KFIRE_GROUP_ID\", name = \"firebase-remoteconfig\", version.ref = \"kfire\" }")
        if (modules.crashlytics) libs.add("kfire-crashlytics = { group = \"$KFIRE_GROUP_ID\", name = \"firebase-crashlytics\", version.ref = \"kfire\" }")
        return libs
    }

    /**
     * Updates root build.gradle.kts to add google-services and crashlytics plugins.
     */
    private fun updateRootBuildGradle(modules: FirebaseModules): Boolean {
        val rootBuildFile = projectRoot.resolve("build.gradle.kts")
        if (!rootBuildFile.exists()) return false

        var content = rootBuildFile.readText()
        var modified = false

        val pluginsBlockRegex = Regex("""(plugins\s*\{[^}]*)(})""", RegexOption.DOT_MATCHES_ALL)
        val match = pluginsBlockRegex.find(content)

        if (match != null) {
            var pluginsContent = match.groupValues[1]
            val closingBrace = match.groupValues[2]

            // Add google-services if not present
            if (!pluginsContent.contains("googleServices")) {
                pluginsContent = pluginsContent.trimEnd() +
                    "\n    alias(libs.plugins.googleServices) apply false\n"
                modified = true
            }

            // Add crashlytics plugin if crashlytics module is selected and not present
            if (modules.crashlytics && !pluginsContent.contains("crashlyticsPlugin")) {
                pluginsContent = pluginsContent.trimEnd() +
                    "\n    alias(libs.plugins.crashlyticsPlugin) apply false\n"
                modified = true
            }

            if (modified) {
                content = content.replace(match.value, pluginsContent + closingBrace)
                rootBuildFile.writeText(content)
            }
        }

        return modified
    }

    /**
     * Finds the app module's build.gradle.kts file.
     */
    private fun findAppBuildFile(): Path? {
        val possiblePaths = listOf(
            "demo/composeApp/build.gradle.kts",
            "composeApp/build.gradle.kts",
            "app/build.gradle.kts",
            "androidApp/build.gradle.kts"
        )

        return possiblePaths
            .map { projectRoot.resolve(it) }
            .firstOrNull { it.exists() }
    }

    /**
     * Updates the app's build.gradle.kts to apply google-services, crashlytics plugin and add Firebase dependencies.
     */
    private fun updateAppBuildGradle(buildFile: Path, modules: FirebaseModules): Boolean {
        var content = buildFile.readText()
        var modified = false

        // 1. Add plugins if not present
        val pluginsBlockRegex = Regex("""(plugins\s*\{[^}]*)(})""", RegexOption.DOT_MATCHES_ALL)
        val pluginsMatch = pluginsBlockRegex.find(content)

        if (pluginsMatch != null) {
            var pluginsContent = pluginsMatch.groupValues[1]
            val closingBrace = pluginsMatch.groupValues[2]
            var pluginsModified = false

            // Add google-services plugin
            if (!pluginsContent.contains("googleServices")) {
                pluginsContent = pluginsContent.trimEnd() +
                    "\n    alias(libs.plugins.googleServices)\n"
                pluginsModified = true
            }

            // Add crashlytics plugin if crashlytics module is selected
            if (modules.crashlytics && !pluginsContent.contains("crashlyticsPlugin")) {
                pluginsContent = pluginsContent.trimEnd() +
                    "\n    alias(libs.plugins.crashlyticsPlugin)\n"
                pluginsModified = true
            }

            if (pluginsModified) {
                content = content.replace(pluginsMatch.value, pluginsContent + closingBrace)
                modified = true
            }
        }

        // 2. Add Firebase dependencies to commonMain using libs catalog
        val firebaseDepsRefs = buildFirebaseDependencyRefs(modules)
        if (firebaseDepsRefs.isNotEmpty() && !content.contains("libs.kfire")) {
            val commonMainDepsRegex = Regex(
                """(commonMain\.dependencies\s*\{[^}]*)(})""",
                RegexOption.DOT_MATCHES_ALL
            )
            val match = commonMainDepsRegex.find(content)

            if (match != null) {
                val depsContent = match.groupValues[1]
                val closingBrace = match.groupValues[2]
                val newDepsContent = depsContent.trimEnd() + "\n\n" +
                    "            // Firebase KMP (KFire)\n" +
                    firebaseDepsRefs.joinToString("\n") { "            implementation($it)" } +
                    "\n        "
                content = content.replace(match.value, newDepsContent + closingBrace)
                modified = true
            }
        }

        // 3. Add coroutines dependency if not present
        if (!content.contains("coroutines")) {
            val commonMainDepsRegex = Regex(
                """(commonMain\.dependencies\s*\{[^}]*)(})""",
                RegexOption.DOT_MATCHES_ALL
            )
            val match = commonMainDepsRegex.find(content)

            if (match != null) {
                val depsContent = match.groupValues[1]
                val closingBrace = match.groupValues[2]
                val newDepsContent = depsContent.trimEnd() +
                    "\n            implementation(libs.kotlinx.coroutines.core)\n        "
                content = content.replace(match.value, newDepsContent + closingBrace)
                modified = true
            }
        }

        if (modified) {
            buildFile.writeText(content)
        }

        return modified
    }

    /**
     * Builds the list of Firebase libs catalog references for build.gradle.kts.
     */
    private fun buildFirebaseDependencyRefs(modules: FirebaseModules): List<String> {
        val refs = mutableListOf<String>()
        if (modules.core) refs.add("libs.kfire.core")
        if (modules.auth) refs.add("libs.kfire.auth")
        if (modules.firestore) refs.add("libs.kfire.firestore")
        if (modules.storage) refs.add("libs.kfire.storage")
        if (modules.messaging) refs.add("libs.kfire.messaging")
        if (modules.analytics) refs.add("libs.kfire.analytics")
        if (modules.remoteConfig) refs.add("libs.kfire.remoteconfig")
        if (modules.crashlytics) refs.add("libs.kfire.crashlytics")
        return refs
    }
}

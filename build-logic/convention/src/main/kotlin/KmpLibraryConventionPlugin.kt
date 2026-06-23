import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
                apply("com.android.kotlin.multiplatform.library")
                apply("org.jetbrains.kotlin.plugin.serialization")
                apply("org.jetbrains.kotlin.native.cocoapods")
            }

            extensions.configure<KotlinMultiplatformExtension> {
                // Suppress expect/actual class warnings
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }

                // iOS targets
                iosArm64()
                iosSimulatorArm64()

                // Pin the JVM bytecode target so published artifacts stay stable
                // regardless of the build JDK (AGP 9 otherwise follows the toolchain).
                targets.configureEach {
                    compilations.configureEach {
                        compileTaskProvider.configure {
                            (compilerOptions as? KotlinJvmCompilerOptions)
                                ?.jvmTarget
                                ?.set(JvmTarget.JVM_11)
                        }
                    }
                }

                sourceSets.apply {
                    // No BOM dependencies - Maven Central requires explicit versions
                }
            }

            // Disable Xcode-related tasks (not needed for library projects without an Xcode project)
            tasks.matching {
                it.name == "checkXcodeProjectConfiguration" ||
                it.name == "convertPbxprojToJson"
            }.configureEach {
                enabled = false
            }
        }
    }
}

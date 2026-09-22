import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.JavadocJar

class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.vanniktech.maven.publish")
            }

            extensions.configure<MavenPublishBaseExtension> {
                publishToMavenCentral()
                signAllPublications()

                configure(KotlinMultiplatform(javadocJar = JavadocJar.Empty()))

                pom {
                    name.set(project.name)
                    description.set("Firebase SDK for Kotlin Multiplatform")
                    inceptionYear.set("2025")
                    url.set("https://github.com/riadmahi/kfire")

                    licenses {
                        license {
                            name.set("Apache License 2.0")
                            url.set("https://opensource.org/licenses/Apache-2.0")
                        }
                    }

                    developers {
                        developer {
                            id.set("riadmahi")
                            name.set("Riad Mahi")
                            url.set("https://github.com/riadmahi")
                        }
                    }

                    scm {
                        url.set("https://github.com/riadmahi/kfire")
                        connection.set("scm:git:git://github.com/riadmahi/kfire.git")
                        developerConnection.set("scm:git:ssh://git@github.com/riadmahi/kfire.git")
                    }
                }
            }
        }
    }
}

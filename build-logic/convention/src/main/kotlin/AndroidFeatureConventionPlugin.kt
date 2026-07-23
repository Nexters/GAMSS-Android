import com.gamss.android.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("gamss.android.library")
                apply("gamss.android.compose")
                apply("gamss.android.hilt")
                apply("org.jetbrains.kotlin.plugin.serialization")
            }
            dependencies {
                "implementation"(libs.findLibrary("androidx-core-ktx").get())
                "implementation"(libs.findLibrary("navigation3-runtime").get())
                "implementation"(libs.findLibrary("kotlinx-serialization-json").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-ktx").get())
                "implementation"(libs.findLibrary("androidx-hilt-navigation-compose").get())
                "implementation"(libs.findLibrary("orbit-core").get())
                "implementation"(libs.findLibrary("orbit-viewmodel").get())
                "implementation"(libs.findLibrary("orbit-compose").get())
                "testImplementation"(libs.findLibrary("junit").get())
                "testImplementation"(libs.findLibrary("orbit-test").get())
                "androidTestImplementation"(libs.findLibrary("androidx-junit").get())
            }
        }
    }
}

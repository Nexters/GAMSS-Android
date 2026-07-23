import com.android.build.api.dsl.ApplicationExtension
import com.gamss.android.buildlogic.configureKotlinAndroidJvmTarget
import com.gamss.android.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> {
                compileSdk = libs.findVersion("android-compileSdk").get().requiredVersion.toInt()
                defaultConfig {
                    minSdk = libs.findVersion("android-minSdk").get().requiredVersion.toInt()
                    targetSdk = libs.findVersion("android-targetSdk").get().requiredVersion.toInt()
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }
            configureKotlinAndroidJvmTarget()
        }
    }
}
